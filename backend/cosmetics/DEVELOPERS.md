# Developer feature access

Deploy `DeveloperRoles.php`, `developer.php` and the updated `public/index.php` to
`/opt/axial-cosmetics/`, preserving the public subdirectory, then reload PHP-FPM.
The role table is created automatically; existing cosmetics are unchanged.
Ship the updated client as well.

On the VPS, assign by the player's full Minecraft account UUID:

```sh
sudo -u www-data php /opt/axial-cosmetics/developer.php grant MINECRAFT-UUID
sudo -u www-data php /opt/axial-cosmetics/developer.php revoke MINECRAFT-UUID
```

The client refreshes every 60 seconds. Only assigned accounts see Replay Mod and
NPC Emotes. Recording, playback and NPC assignment also check the role.
Normal player emotes remain available. Roles default to denied at startup and
are never restored from the cosmetics disk cache. During an API outage the
current session retains its last confirmed role until the next successful fetch.
This controls features in the shipped client; modified clients can bypass local
checks. Do not use this role to authorize privileged game-server operations.
