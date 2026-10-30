#!/usr/bin/env bash
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive
apt-get install -y -qq python3-venv
id axial-emotes >/dev/null 2>&1 || useradd --system --no-create-home --shell /usr/sbin/nologin axial-emotes
install -d -o root -g root -m 0755 /opt/axial-emotes
install -o root -g root -m 0644 "$(dirname "$0")/relay.py" /opt/axial-emotes/relay.py
install -o root -g root -m 0644 "$(dirname "$0")/requirements.txt" /opt/axial-emotes/requirements.txt
python3 -m venv /opt/axial-emotes/venv
/opt/axial-emotes/venv/bin/pip install --disable-pip-version-check -r /opt/axial-emotes/requirements.txt
install -o root -g root -m 0644 "$(dirname "$0")/axial-emotes.service" /etc/systemd/system/axial-emotes.service
systemctl daemon-reload
systemctl enable --now axial-emotes
systemctl restart axial-emotes
# Add Caddyfile.snippet to the existing site config, validate, and reload Caddy separately.
