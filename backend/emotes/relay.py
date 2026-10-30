"""Authenticated, in-memory Emotecraft transport. Never accepts a Minecraft access token."""
import asyncio
import base64
import contextlib
import ipaddress
import json
import logging
import re
import secrets
import signal
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from collections import defaultdict, deque
from dataclasses import dataclass, field
from websockets.asyncio.server import serve
from websockets.exceptions import ConnectionClosed

MAX_PACKET = 262144
MAX_MESSAGE = 360000
MAX_WATCH = 256
UUID_RE = re.compile(r"[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}\Z")
NAME_RE = re.compile(r"[A-Za-z0-9_]{3,16}\Z")

def canonical_uuid(value):
    if not isinstance(value, str) or not UUID_RE.fullmatch(value):
        raise ValueError("Invalid UUID")
    return value

def encode(message):
    return json.dumps(message, separators=(",", ":"))

async def verify_minecraft(name, server_id, expected_uuid):
    """The client proves account ownership to Mojang; only its public proof is checked here."""
    def fetch():
        url = "https://sessionserver.mojang.com/session/minecraft/hasJoined?" + urllib.parse.urlencode({"username": name, "serverId": server_id})
        try:
            with urllib.request.urlopen(url, timeout=6) as response:
                if response.status != 200:
                    return False
                body = response.read(32769)
                if len(body) > 32768:
                    return False
                profile = json.loads(body)
                return str(uuid.UUID(profile["id"])) == expected_uuid and profile["name"].lower() == name.lower()
        except (OSError, ValueError, KeyError, TypeError):
            return False
    return await asyncio.to_thread(fetch)

@dataclass(eq=False)
class Peer:
    socket: object
    uuid: str
    watch: set = field(default_factory=set)
    queue: asyncio.Queue = field(default_factory=lambda: asyncio.Queue(16))
    queued_bytes: int = 0
    tokens: float = 40
    byte_tokens: float = 2 * 1024 * 1024
    budget_at: float = field(default_factory=time.monotonic)

    def push(self, message):
        text = encode(message)
        size = len(text)
        if self.queue.full() or self.queued_bytes + size > 1024 * 1024:
            asyncio.create_task(self.socket.close(1013, "Receiver too slow"))
            return
        self.queued_bytes += size
        self.queue.put_nowait((text, size))

    async def write(self):
        while True:
            text, size = await self.queue.get()
            try:
                await asyncio.wait_for(self.socket.send(text), 10)
            finally:
                self.queued_bytes -= size

    def consume(self, size):
        now = time.monotonic()
        elapsed = now - self.budget_at
        self.budget_at = now
        self.tokens = min(40, self.tokens + elapsed * 20)
        self.byte_tokens = min(2 * 1024 * 1024, self.byte_tokens + elapsed * 256 * 1024)
        if self.tokens < 1 or self.byte_tokens < size:
            raise ValueError("Rate limit")
        self.tokens -= 1
        self.byte_tokens -= size

class Relay:
    def __init__(self, verifier=verify_minecraft):
        self.peers = {}
        self.verifier = verifier
        self.auth_slots = asyncio.Semaphore(16)
        self.attempts = defaultdict(deque)
        self.open_connections = 0

    def ip(self, socket):
        # Caddy overwrites this header; the service binds to loopback only.
        raw = socket.request.headers.get("X-Real-IP") or socket.remote_address[0]
        return str(ipaddress.ip_address(raw))

    def allow_connection(self, ip):
        now = time.monotonic()
        if len(self.attempts) > 4096:
            for key in list(self.attempts):
                if not self.attempts[key] or now - self.attempts[key][-1] > 300:
                    del self.attempts[key]
            if len(self.attempts) > 4096:
                return False
        recent = self.attempts[ip]
        while recent and now - recent[0] > 300:
            recent.popleft()
        if len(recent) >= 60:
            return False
        recent.append(now)
        return True

    async def process_request(self, socket, request):
        if request.path in ("/health", "/v1/emotes/health"):
            response = socket.respond(200, '{"status":"ok"}\n')
            response.headers["Content-Type"] = "application/json"
            return response
        if request.path != "/v1/emotes":
            return socket.respond(404, "Not found\n")
        try:
            allowed = self.open_connections < 2000 and self.allow_connection(self.ip(socket))
        except (ValueError, KeyError):
            allowed = False
        if not allowed:
            return socket.respond(429, "Try again later\n")

    async def handler(self, socket):
        peer = None
        writer = None
        self.open_connections += 1
        try:
            challenge = secrets.token_hex(20)
            await socket.send(encode({"type": "challenge", "version": 1, "serverId": challenge}))
            raw = await asyncio.wait_for(socket.recv(), 20)
            if not isinstance(raw, str) or len(raw) > 1024:
                raise ValueError("Invalid authentication")
            message = json.loads(raw)
            if not isinstance(message, dict) or message.get("type") != "auth" or message.get("version") != 1:
                raise ValueError("Authentication required")
            player = canonical_uuid(message.get("uuid"))
            name = message.get("name")
            if not isinstance(name, str) or not NAME_RE.fullmatch(name):
                raise ValueError("Invalid username")
            async with self.auth_slots:
                valid = await asyncio.wait_for(self.verifier(name, challenge, player), 8)
            if not valid:
                raise ValueError("Minecraft authentication failed")
            peer = Peer(socket, player)
            old = self.peers.get(player)
            self.peers[player] = peer
            if old:
                await old.socket.close(1000, "Account reconnected")
            writer = asyncio.create_task(peer.write())
            peer.push({"type": "ready", "version": 1})
            async with asyncio.timeout(21600):
                async for raw in socket:
                    if not isinstance(raw, str):
                        raise ValueError("Text messages required")
                    peer.consume(len(raw))
                    self.message(peer, json.loads(raw))
        except (ValueError, TypeError, KeyError, TimeoutError):
            await socket.close(1008, "Invalid or unauthenticated relay message")
        except ConnectionClosed:
            pass
        finally:
            self.open_connections -= 1
            if writer:
                writer.cancel()
                with contextlib.suppress(asyncio.CancelledError, ConnectionClosed, TimeoutError):
                    await writer
            if peer and self.peers.get(peer.uuid) is peer:
                del self.peers[peer.uuid]
                for other in self.peers.values():
                    if peer.uuid in other.watch:
                        other.push({"type": "gone", "uuid": peer.uuid})

    def message(self, peer, message):
        if not isinstance(message, dict):
            raise ValueError("Invalid message")
        if message.get("type") == "watch":
            values = message.get("players")
            if not isinstance(values, list) or len(values) > MAX_WATCH:
                raise ValueError("Invalid visible players")
            watch = {canonical_uuid(value) for value in values} - {peer.uuid}
            added = watch - peer.watch
            peer.watch = watch
            # Ask for live animation/tick snapshots; never replay stale cached starts.
            for player in added:
                other = self.peers.get(player)
                if other and peer.uuid in other.watch:
                    other.push({"type": "request", "uuid": peer.uuid})
                    peer.push({"type": "request", "uuid": other.uuid})
            return
        if message.get("type") != "packet":
            raise ValueError("Invalid message type")
        data = message.get("data")
        if not isinstance(data, str) or len(data) > 349528:
            raise ValueError("Invalid packet size")
        packet = base64.b64decode(data, validate=True)
        if not 0 < len(packet) <= MAX_PACKET:
            raise ValueError("Invalid packet size")
        target = message.get("target")
        if target is not None:
            target = canonical_uuid(target)
        recipients = [self.peers[target]] if target in self.peers else ([] if target else list(self.peers.values()))
        for other in recipients:
            if other is not peer and peer.uuid in other.watch and other.uuid in peer.watch:
                # Identity is always from the verified connection, never a packet's claimed author.
                other.push({"type": "packet", "uuid": peer.uuid, "data": data})

async def main():
    relay = Relay()
    stop = asyncio.Event()
    loop = asyncio.get_running_loop()
    for sig in (signal.SIGTERM, signal.SIGINT):
        loop.add_signal_handler(sig, stop.set)
    async with serve(relay.handler, "127.0.0.1", 8765, process_request=relay.process_request,
                     max_size=MAX_MESSAGE, max_queue=8, compression=None, ping_interval=20,
                     ping_timeout=20, close_timeout=3, server_header=None):
        logging.info("Axial emote relay listening on loopback")
        await stop.wait()

if __name__ == "__main__":
    logging.basicConfig(level=logging.WARNING, format="%(levelname)s %(message)s")
    asyncio.run(main())
