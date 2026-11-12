PRAGMA secure_delete=ON;
CREATE TABLE IF NOT EXISTS feedback (id INTEGER PRIMARY KEY AUTOINCREMENT,uuid TEXT NOT NULL,username TEXT NOT NULL,message TEXT NOT NULL,request_id TEXT NOT NULL,created_at INTEGER NOT NULL,status TEXT NOT NULL DEFAULT 'pending',UNIQUE(uuid,request_id));
CREATE TABLE IF NOT EXISTS feedback_notifications (id INTEGER PRIMARY KEY AUTOINCREMENT,uuid TEXT NOT NULL,feedback_id INTEGER NOT NULL UNIQUE,reply TEXT NOT NULL);
CREATE INDEX IF NOT EXISTS feedback_notifications_player ON feedback_notifications(uuid,id);
CREATE TABLE IF NOT EXISTS feedback_challenges (challenge TEXT PRIMARY KEY,expires_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS feedback_sessions (token TEXT PRIMARY KEY,uuid TEXT NOT NULL,username TEXT NOT NULL,created_at INTEGER NOT NULL,expires_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS feedback_limits (peer TEXT PRIMARY KEY,started_at INTEGER NOT NULL,attempts INTEGER NOT NULL);
