#!/usr/bin/env bash
set -euo pipefail

# Ubuntu 22.04/24.04 VPS Provisioner for PRANAV Backend + Nginx + Certbot + UFW Firewall
# Execute on your VPS as root: sudo bash setup-vps-ubuntu.sh

if [ "$(id -u)" -ne 0 ]; then
  echo "Run as root (sudo bash setup-vps-ubuntu.sh)"
  exit 1
fi

apt-get update && apt-get install -y curl gnupg nginx certbot python3-certbot-nginx ufw logrotate

if ! command -v node >/dev/null 2>&1; then
  curl -fsSL https://deb.nodesource.com/setup_22.x | bash -
  apt-get install -y nodejs
fi

id -u pranav >/dev/null 2>&1 || useradd -r -s /usr/sbin/nologin pranav
mkdir -p /opt/pranav/backend /var/lib/pranav/data /var/log/pranav
chown -R pranav:pranav /var/lib/pranav /var/log/pranav

ufw allow OpenSSH
ufw allow 'Nginx Full'
ufw --force enable

echo "VPS prerequisites installed. Next steps:"
echo "1. Copy PRANAV/backend to /opt/pranav/backend"
echo "2. Copy .env.production.example to /opt/pranav/.env.production and generate secrets via 'openssl rand -hex 32'"
echo "3. Copy nginx/pranavsmm.shop.conf to /etc/nginx/sites-available/ && symlink into sites-enabled/"
echo "4. Run: certbot --nginx -d pranavsmm.shop -d admin.pranavsmm.shop -d api.pranavsmm.shop -d bot.pranavsmm.shop"
echo "5. Enable service: cp systemd/pranav-backend.service /etc/systemd/system/ && systemctl enable --now pranav-backend"
