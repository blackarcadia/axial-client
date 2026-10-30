#!/usr/bin/env bash
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq caddy php-fpm php-sqlite3 php-cli sqlite3
install -d -o root -g www-data -m 0750 /etc/axial-cosmetics
install -d -o www-data -g www-data -m 0750 /var/lib/axial-cosmetics
install -d -o root -g www-data -m 0755 /opt/axial-cosmetics/public/assets
# Source files are uploaded separately before initialization.
printf 'Backend runtime installed.\n'
