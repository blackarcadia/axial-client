# Global emote relay

`wss://api.axialclient.com/v1/emotes` connects updated AxialClient users on Minecraft servers without native Emotecraft synchronization. Both viewers and performers need the updated client and a valid Minecraft Java session. This cannot make unmodified Minecraft clients render emotes.

The client registers an Emotecraft proxy and uses its existing start/stop animation packets. Native Emotecraft server networking retains priority, including Axial Prisons. The relay does not change the Minecraft server or the Prisons store/website. Existing rendering hides equipment and capes during relayed emotes through the same Emotecraft player state as native emotes.

## Authentication and delivery

1. After a world loads and settles for five seconds, the client opens WSS.
2. The VPS sends a fresh 40-character challenge. The client calls Mojang's session `joinServer` API with its normal session credentials. Those credentials are sent only to Mojang through authlib, never to the VPS.
3. The VPS calls Mojang `hasJoined` and binds the socket to the verified profile UUID. Unverified sockets cannot subscribe or publish. The proof is unique per connection and is not reused as a bearer token.
4. Each client sends at most 256 currently loaded player UUIDs once per second when the set changes. No game-server address, chat, world coordinates, Microsoft tokens or Minecraft tokens are sent to this service.
5. Emote packets are sent only between mutually subscribed, verified accounts. The envelope UUID always comes from the authenticated socket. Embedded packet UUIDs are overridden at the receiver; forced emotes, file transfers and config packets are not accepted from peers.
6. New viewers and reconnecting clients request a current animation/tick snapshot rather than replaying stale starts. Disconnects stop relay-owned animations; world changes close the socket and clear subscriptions. Network/auth failures back off to a maximum 60 seconds and do not block the game thread or prevent local emotes.

The proxy supports the six bundled emotes and other compatible Emotecraft animations within the 256 KiB packet limit. Larger imported animations may not synchronize. This is transport, not a paid-emote entitlement service. Offline/invalid Minecraft sessions cannot authenticate. The relay keeps no persistent emote or player database.

## Deploy

Run `sudo bash backend/emotes/install.sh` from an uploaded checkout. It installs a dedicated unprivileged `axial-emotes` service, root-owned code and a Python virtual environment in `/opt/axial-emotes`. The backend listens only on `127.0.0.1:8765`. Add `Caddyfile.snippet` inside the existing API hostname site block. It can also go in the shared temporary hostname block. Validate with `sudo caddy validate --config /etc/caddy/Caddyfile`, then reload Caddy.

Caddy must overwrite `X-Real-IP` as shown; the service uses that trusted proxy header for a connection attempt limit. Keep port 8765 private. Limits include authentication timeouts and concurrency, maximum connections, message/byte rate budgets, packet sizes, bounded outgoing queues and a systemd memory limit. No shared client secret or relay admin API key is needed.

Health: `https://api.axialclient.com/v1/emotes/health` (also `http://127.0.0.1:8765/health` locally). Operations: `systemctl status axial-emotes`, `journalctl -u axial-emotes`, `systemctl restart axial-emotes`. Updating requires replacing `relay.py` and restarting the service; no database migration is involved.

## Validation

`venv/bin/python -m unittest -v test_relay` runs real local WebSocket conversations against an injected test verifier. It verifies rejection before authentication, identity binding, mutual visibility, targeted routing, new-viewer snapshots, duplicate-session replacement, disconnect cleanup, bounds and rate limiting. The production entry point always uses Mojang verification and has no environment-based authentication bypass.

Client tests cover relay envelope bounds and the actual Emotecraft start/stop wire format for all six bundled animations. The production WSS handshake and rejection of unauthenticated frames are checked separately. Final visual multiplayer validation requires two updated clients with valid accounts on the same non-Prisons Minecraft server: play/stop an emote, enter view mid-emote, reconnect, change worlds, and verify cape/equipment restoration. Synthetic tests do not establish visual two-account behavior.

Emotecraft's [proxy API](https://github.com/KosmX/emotes-proxy-template) provides server-independent transport. The dedicated relay uses [websockets asyncio](https://websockets.readthedocs.io/en/15.0.1/reference/asyncio/server.html).
