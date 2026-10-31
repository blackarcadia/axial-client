PRAGMA journal_mode=WAL;
CREATE TABLE IF NOT EXISTS grants (uuid TEXT NOT NULL, cape TEXT NOT NULL, PRIMARY KEY(uuid,cape));
CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, updated_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS audit (id INTEGER PRIMARY KEY AUTOINCREMENT, created_at INTEGER NOT NULL, uuid TEXT NOT NULL, action TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS login_limits (peer TEXT PRIMARY KEY, started_at INTEGER NOT NULL, attempts INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS metadata (name TEXT PRIMARY KEY, value INTEGER NOT NULL);
INSERT OR IGNORE INTO metadata VALUES ('revision',1);
CREATE TABLE IF NOT EXISTS paynow_secrets (event_type TEXT PRIMARY KEY, secret TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS paynow_events (event_id TEXT PRIMARY KEY, event_type TEXT NOT NULL, received_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS paynow_orders (order_id TEXT PRIMARY KEY, total INTEGER NOT NULL, created_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS paynow_purchases (order_id TEXT NOT NULL, line_id TEXT NOT NULL, uuid TEXT NOT NULL, cape TEXT NOT NULL, PRIMARY KEY(order_id,line_id));
CREATE TABLE IF NOT EXISTS paynow_refunds (refund_id TEXT PRIMARY KEY, order_id TEXT NOT NULL, amount INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS paynow_chargebacks (payment_id TEXT PRIMARY KEY, order_id TEXT NOT NULL);
CREATE VIEW IF NOT EXISTS effective_grants AS
 SELECT uuid,cape FROM grants UNION
 SELECT p.uuid,p.cape FROM paynow_purchases p JOIN paynow_orders o USING(order_id)
 WHERE NOT EXISTS (SELECT 1 FROM paynow_chargebacks c WHERE c.order_id=p.order_id)
 AND NOT EXISTS (SELECT 1 FROM paynow_refunds r WHERE r.order_id=p.order_id
                 GROUP BY r.order_id HAVING SUM(r.amount)>=o.total);
