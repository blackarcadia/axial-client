# Cosmetic assignments

Admin: https://api.axialclient.com/admin

Fallback: https://37-59-98-176.sslip.io/admin

Permanent address: https://api.axialclient.com/admin once the `api` A record points to `37.59.98.176` (DNS-only if using Cloudflare). HTTPS certificates are managed by Caddy. The temporary HTTPS address is live while DNS is pending.

The admin password is stored privately on the owner's Mac in `/Users/blackarcadia/AxialPrivate/axial-cosmetics-admin.txt`. It is not in this repository or shipped in the client.

## Assign or remove cosmetics

1. Sign in, paste the player's Minecraft UUID and click **Load player**.
2. Check the capes they should own; uncheck anything to revoke it.
3. Click **Save cosmetic access**.

Saving replaces that player's entire selection of grants. Unchecking every cape removes manual access; active purchases continue to grant their cosmetics. Existing assignments were seeded without changing their original owners. The available designs are Gold Owner, Admin, Staff, Lunar Raven the Hexwind Halloween and Gold Spark auras, and Swim Bands (Outfits). See [Hexwind setup](HEXWIND.md) and [Gold Spark setup](GOLD_SPARK.md) and [Swim Bands setup](SWIM_BANDS.md). Adding a new cape design still requires a client update.

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

This service uses SQLite on the VPS; the website's existing MySQL presence counter remains separate. Admin writes require a session, a CSRF token and HTTPS. Login attempts are rate limited. PayNow signed purchase notifications can grant the configured cosmetics; see below.

## Deploy updates

Copy this directory's contents into `/opt/axial-cosmetics` using SFTP/SSH, excluding any local secret files. Preserve `/etc/axial-cosmetics` and `/var/lib/axial-cosmetics`; they contain live configuration and grants. Do not overwrite the database with `seed.json`.

For initial installation only, run `sudo bash /opt/axial-cosmetics/install.sh`. Create a root-readable `/tmp/axial-cosmetics-bootstrap.json` containing an `admin_password` value, then run `sudo php /opt/axial-cosmetics/initialize.php` and delete the bootstrap file immediately. `initialize.php` seeds only a newly created database and preserves existing grants. Set database ownership to `www-data:www-data` with mode `0640`. Add Caddy to the `www-data` group for access to the PHP-FPM socket. Configure the hostname in Caddy, validate with `sudo caddy validate --config /etc/caddy/Caddyfile`, then reload/restart Caddy and PHP-FPM after deploying PHP changes (clears opcode caches).

Check `https://api.axialclient.com/health` or the temporary hostname above. Never put config, bootstrap credentials, or the database under `public/`.

## Backups and recovery

`axial-cosmetics-backup.timer` runs `backup.sh` daily. Backups live in `/var/backups/axial-cosmetics`, readable only by root, with 14 days retained. These are local recovery copies; periodically copy them and the private config off the VPS for protection against disk loss.

Run a backup manually with `sudo bash /opt/axial-cosmetics/backup.sh`. To restore, stop PHP-FPM, preserve a copy of the current database, replace the database with a verified backup, remove its stale `-wal`/`-shm` sidecars while PHP is stopped, restore owner `www-data:www-data` and mode `0640`, then start PHP-FPM. Never replace a database while requests are writing to it.

## PayNow purchases

Existing Axial Prisons store: `559186693647761408`. `paynow.json` maps product `619762575072043008` to `staff`. Keep the current Prisons store/business and game-server configuration. This integration consumes signed JSON notifications and fulfills cosmetics itself; it is not a PayNow gameserver plugin or a change to the store's delivery business type. Confirm fulfillment with a real zero-cost checkout before making the product available for paid purchases.

In PayNow → Integrations → Webhooks, create three **JSON** subscriptions, one per event:

- Order Completed (`ON_ORDER_COMPLETED`)
- Refund (`ON_REFUND`)
- Chargeback (`ON_CHARGEBACK`)

Use `https://api.axialclient.com/webhooks/paynow` for all three. Each subscription has its own signing secret. In our admin page's **PayNow connection** section, choose the matching event and save that subscription's signing secret. Secrets are stored in the private SQLite database, never echoed into HTML or included in the public ownership endpoint. No PayNow management API key is required for this notification-based flow. Missing secrets return HTTP 503 so PayNow can retry. Configure all three before selling.

Only completed one-time orders from the configured store grant access. Unknown products are ignored. Gift purchases go to `gift_to_customer.minecraft.id`, otherwise the order customer's Minecraft UUID. Both compact and hyphenated UUIDs are accepted. A mapped line without a valid UUID is rejected rather than delivered to an unverified name. Subscriptions/trials are not supported.

Raw request bytes and millisecond timestamp are verified with PayNow's Base64 SHA-256 HMAC signature. Timestamps outside a five-minute window are rejected. Event deduplication and entitlement changes share one transaction. The order and line IDs provide a second layer against duplicate purchases. No card, billing name/email, address, raw webhook body or customer token is stored.

`grants` remains manual access. The `effective_grants` view unions it with active `paynow_purchases`, preserving manual grants and other valid purchases when an order is refunded or charged back. Completed refund amounts accumulate by unique refund ID. Full order refunds revoke that order's cosmetics. Partial refunds retain access and are flagged for review because the refund event does not identify which order line was refunded. A chargeback blocks all cosmetic lines from that order; reinstatement after a won dispute requires review/manual access. Refunds/chargebacks arriving before the order are retained so late order delivery cannot accidentally regrant access.

The admin page shows configured secret statuses and recent verified events. To test: use a zero-price product or a coupon reducing the total to zero, buy through the actual storefront with a test Minecraft account, confirm an Order Completed event appears and Staff appears in its wardrobe within a minute. Exercise refunds on a test order as well. Synthetic tests do not replace this real PayNow delivery check.

Run backend tests with `php backend/cosmetics/tests/paynow.php` (PHP 8.2+ and PDO SQLite). These use an in-memory database and test-only signing secrets, never live credentials or grants. Run `initialize.php` on upgrades before switching to the new `public/index.php` to install additive schema changes. Existing manual grants remain intact.

References: [signature verification](https://docs.paynow.gg/webhooks/validating-incoming-webhooks/), [replay protection](https://docs.paynow.gg/webhooks/preventing-replay-attacks/), [order payload](https://docs.paynow.gg/webhooks/events/on-order-completed/), [refund payload](https://docs.paynow.gg/webhooks/events/on-refund/).
