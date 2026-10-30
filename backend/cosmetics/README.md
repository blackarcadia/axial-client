# Cosmetic assignments

Admin: https://37-59-98-176.sslip.io/admin

Permanent address: https://api.axialclient.com/admin once the `api` A record points to `37.59.98.176` (DNS-only if using Cloudflare). HTTPS certificates are managed by Caddy. The temporary HTTPS address is live while DNS is pending.

The admin password is stored privately on the owner's Mac in `/Users/blackarcadia/AxialPrivate/axial-cosmetics-admin.txt`. It is not in this repository or shipped in the client.

## Assign or remove cosmetics

1. Sign in, paste the player's Minecraft UUID and click **Load player**.
2. Check the capes they should own; uncheck anything to revoke it.
3. Click **Save cosmetic access**.

Saving replaces that player's entire selection of grants. Unchecking every cape removes wardrobe access. Existing assignments were seeded without changing their original owners. The available designs are Gold Owner, Admin, Staff and Lunar Raven. Adding a new cape design still requires a client update.

Updated clients fetch grants on launch and every 60 seconds. Open wardrobes refresh when grants change. All updated clients receive the same ownership list for rendering other players. Equipped choices remain local per account, as before; this does not synchronize a player's chosen cape between machines/viewers. Older client builds keep their bundled assignments until upgraded.

The client stores its last valid snapshot in `config/axial-cosmetic-grants-cache.json` and retains it during outages. Before its first successful fetch, bundled grants remain available. The client never contains an admin password and cannot write grants. Its local cache is not an anti-tamper or payment entitlement mechanism.

## Server

- Source: `/opt/axial-cosmetics`, web root: `/opt/axial-cosmetics/public`
- Private password hash/settings: `/etc/axial-cosmetics/config.php`
- SQLite database: `/var/lib/axial-cosmetics/cosmetics.sqlite`
- Services: `caddy`, `php8.5-fpm`
- Caddy config: `/etc/caddy/Caddyfile` (example alongside this README)
- Health: `GET /health`
- Public ownership snapshot: `GET /v1/cosmetics` with revision ETag and conditional GET support
- Audit trail: `audit` SQLite table

This service uses SQLite on the VPS; the website's existing MySQL presence counter remains separate. Admin writes require a session, a CSRF token and HTTPS. Login attempts are rate limited. No payment flow is included.

## Deploy updates

Copy this directory's contents into `/opt/axial-cosmetics` using SFTP/SSH, excluding any local secret files. Preserve `/etc/axial-cosmetics` and `/var/lib/axial-cosmetics`; they contain live configuration and grants. Do not overwrite the database with `seed.json`.

For initial installation only, run `sudo bash /opt/axial-cosmetics/install.sh`. Create a root-readable `/tmp/axial-cosmetics-bootstrap.json` containing an `admin_password` value, then run `sudo php /opt/axial-cosmetics/initialize.php` and delete the bootstrap file immediately. `initialize.php` seeds only a newly created database and preserves existing grants. Set database ownership to `www-data:www-data` with mode `0640`. Add Caddy to the `www-data` group for access to the PHP-FPM socket. Configure the hostname in Caddy, validate with `sudo caddy validate --config /etc/caddy/Caddyfile`, then reload/restart Caddy and PHP-FPM after deploying PHP changes (clears opcode caches).

Check `https://api.axialclient.com/health` or the temporary hostname above. Never put config, bootstrap credentials, or the database under `public/`.

## Backups and recovery

`axial-cosmetics-backup.timer` runs `backup.sh` daily. Backups live in `/var/backups/axial-cosmetics`, readable only by root, with 14 days retained. These are local recovery copies; periodically copy them and the private config off the VPS for protection against disk loss.

Run a backup manually with `sudo bash /opt/axial-cosmetics/backup.sh`. To restore, stop PHP-FPM, preserve a copy of the current database, replace the database with a verified backup, remove its stale `-wal`/`-shm` sidecars while PHP is stopped, restore owner `www-data:www-data` and mode `0640`, then start PHP-FPM. Never replace a database while requests are writing to it.
