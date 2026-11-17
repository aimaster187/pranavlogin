# Backend Deployment Guide (Ubuntu 22.04 / 24.04 VPS)

All deployment artifacts live under `/PRANAV/deployment/`:
- `.env.development.example`, `.env.staging.example`, `.env.production.example`
- `nginx/pranavsmm.shop.conf`
- `systemd/pranav-backend.service`
- `scripts/setup-vps-ubuntu.sh`, `scripts/backup-and-restore.sh`, `scripts/rollback.sh`

## Installation Steps on Your Live VPS
1. Copy `PRANAV/backend` to `/opt/pranav/backend`.
2. Run `sudo bash PRANAV/deployment/scripts/setup-vps-ubuntu.sh`.
3. Create `/opt/pranav/.env.production` from `.env.production.example` and generate strong secrets:
   ```bash
   openssl rand -hex 32   # OWNER_API_KEY
   openssl rand -hex 32   # VAULT_MASTER_KEY
   ```
4. Install `/etc/systemd/system/pranav-backend.service` and start:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable --now pranav-backend
   curl -sS http://127.0.0.1:8080/api/v1/health
   ```
5. Enter `https://api.pranavsmm.shop` and your `OWNER_API_KEY` inside **PRANAV Admin -> Bots & Ops -> VPS & Auth**.
