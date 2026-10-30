#!/usr/bin/env bash
set -euo pipefail
# Run as root; SQLite's backup API includes committed WAL transactions.
install -d -o root -g root -m 0700 /var/backups/axial-cosmetics
umask 077
backup_file="/var/backups/axial-cosmetics/cosmetics-$(date -u +%Y%m%dT%H%M%SZ).sqlite"
sqlite3 /var/lib/axial-cosmetics/cosmetics.sqlite ".backup '$backup_file'"
find /var/backups/axial-cosmetics -name 'cosmetics-*.sqlite' -type f -mtime +14 -delete
