import asyncio
import base64
import contextlib
import json
import unittest
from websockets.asyncio.client import connect
from websockets.asyncio.server import serve
from websockets.exceptions import ConnectionClosed
from relay import Relay, MAX_MESSAGE, Peer, canonical_uuid

A = '00000000-0000-4000-8000-000000000001'
B = '00000000-0000-4000-8000-000000000002'
C = '00000000-0000-4000-8000-000000000003'

class RelayTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        async def verify(name, challenge, player):
            return len(challenge) == 40 and name == 'Verified' and player in (A, B, C)
        self.relay = Relay(verify)
        self.server = await serve(self.relay.handler, '127.0.0.1', 0, max_size=MAX_MESSAGE, compression=None)
        self.url = 'ws://127.0.0.1:' + str(self.server.sockets[0].getsockname()[1]) + '/v1/emotes'
        self.sockets = []

    async def asyncTearDown(self):
        for socket in self.sockets:
            await socket.close()
        self.server.close()
        await self.server.wait_closed()

    async def client(self, player, name='Verified'):
        socket = await connect(self.url, compression=None)
        self.sockets.append(socket)
        challenge = json.loads(await socket.recv())
        self.assertEqual(challenge['type'], 'challenge')
        await socket.send(json.dumps({'type': 'auth', 'version': 1, 'uuid': player, 'name': name}))
        if name == 'Verified':
            self.assertEqual(json.loads(await socket.recv())['type'], 'ready')
        return socket

    async def send(self, socket, value):
        await socket.send(json.dumps(value))

    async def recv_type(self, socket, kind):
        async with asyncio.timeout(2):
            while True:
                message = json.loads(await socket.recv())
                if message['type'] == kind:
                    return message

    async def pair(self):
        a, b = await self.client(A), await self.client(B)
        await self.send(a, {'type': 'watch', 'players': [B]})
        await self.send(b, {'type': 'watch', 'players': [A]})
        self.assertEqual((await self.recv_type(a, 'request'))['uuid'], B)
        self.assertEqual((await self.recv_type(b, 'request'))['uuid'], A)
        return a, b

    async def test_unverified_or_preauth_packet_never_publishes(self):
        socket = await self.client(A, 'Unverified')
        with self.assertRaises(ConnectionClosed):
            await socket.recv()
        other = await connect(self.url)
        self.sockets.append(other)
        await other.recv()
        await self.send(other, {'type': 'packet', 'uuid': A, 'data': 'AA=='})
        with self.assertRaises(ConnectionClosed):
            await other.recv()
        self.assertFalse(self.relay.peers)

    async def test_verified_identity_overrides_spoofed_uuid_and_only_visible_peers_receive(self):
        a, b = await self.pair()
        c = await self.client(C)
        await self.send(a, {'type': 'packet', 'uuid': C, 'data': 'AQID'})
        packet = await self.recv_type(b, 'packet')
        self.assertEqual(packet, {'type': 'packet', 'uuid': A, 'data': 'AQID'})
        with self.assertRaises(TimeoutError):
            await asyncio.wait_for(c.recv(), .1)

    async def test_subscription_is_mutual_and_targeted_packets_cannot_leak(self):
        a, b = await self.pair()
        c = await self.client(C)
        await self.send(c, {'type': 'watch', 'players': [A]})
        await self.send(a, {'type': 'packet', 'target': C, 'data': 'AQID'})
        with self.assertRaises(TimeoutError):
            await asyncio.wait_for(c.recv(), .1)
        with self.assertRaises(TimeoutError):
            await asyncio.wait_for(b.recv(), .1)
        await self.send(a, {'type': 'packet', 'target': B, 'data': 'AQID'})
        self.assertEqual((await self.recv_type(b, 'packet'))['uuid'], A)

    async def test_disconnect_stops_observers_and_duplicate_account_replaces_old_socket(self):
        a, b = await self.pair()
        replacement = await self.client(A)
        with self.assertRaises(ConnectionClosed):
            await a.recv()
        self.assertIsNotNone(self.relay.peers[A])
        await self.send(replacement, {'type': 'watch', 'players': [B]})
        await self.recv_type(b, 'request')
        await replacement.close()
        self.assertEqual((await self.recv_type(b, 'gone'))['uuid'], A)

    async def test_late_subscribers_request_current_tick_and_no_state_is_cached(self):
        a = await self.client(A)
        await self.send(a, {'type': 'watch', 'players': [B]})
        await self.send(a, {'type': 'packet', 'data': 'AQID'})
        b = await self.client(B)
        await self.send(b, {'type': 'watch', 'players': [A]})
        self.assertEqual((await self.recv_type(a, 'request'))['uuid'], B)
        self.assertEqual((await self.recv_type(b, 'request'))['uuid'], A)
        with self.assertRaises(TimeoutError):
            await asyncio.wait_for(b.recv(), .1)

    async def test_bad_uuid_packet_bounds_and_rate_limits(self):
        with self.assertRaises(ValueError):
            canonical_uuid('not-a-uuid')
        a = await self.client(A)
        await self.send(a, {'type': 'watch', 'players': [B] * 257})
        with self.assertRaises(ConnectionClosed):
            await a.recv()
        a = await self.client(A)
        await self.send(a, {'type': 'packet', 'data': base64.b64encode(bytes(262145)).decode()})
        with self.assertRaises(ConnectionClosed):
            await a.recv()
        peer = Peer(None, A)
        peer.consume(2 * 1024 * 1024)
        with self.assertRaises(ValueError):
            peer.consume(262144)

if __name__ == '__main__':
    unittest.main()
