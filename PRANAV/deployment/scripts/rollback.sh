#!/usr/bin/env bash
set -euo pipefail

# Clean rollback script for PRANAV VPS backend and Nginx configuration
APP_DIR="/opt/pranav/backend"
PREV_DIR="/opt/pranav/backend.prev"

if [ ! -d "$PREV_DIR" ]; then
  echo "No previous release snapshot found at $PREV_DIR"
  exit 1
fi

systemctl stop pranav-backend
rm -rf "${APP_DIR}.failed"
mv "$APP_DIR" "${APP_DIR}.failed"
mv "$PREV_DIR" "$APP_DIR"
systemctl start pranav-backend
curl -fsS http://127.0.0.1:8080/api/v1/health
echo ""
echo "Rollback complete and health endpoint verified."
