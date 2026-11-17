#!/usr/bin/env bash
set -euo pipefail

DATA_DIR="${DATA_DIR:-/var/lib/pranav/data}"
BACKUP_DIR="${BACKUP_DIR:-/var/lib/pranav/backups}"
mkdir -p "$BACKUP_DIR"

MODE="${1:-backup}"
if [ "$MODE" = "backup" ]; then
  STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
  ARCHIVE="$BACKUP_DIR/pranav_backup_${STAMP}.tar.gz"
  tar -czf "$ARCHIVE" -C "$DATA_DIR" .
  echo "Created backup: $ARCHIVE"
elif [ "$MODE" = "restore" ]; then
  ARCHIVE="${2:?Provide backup tar.gz path}"
  systemctl stop pranav-backend || true
  tar -xzf "$ARCHIVE" -C "$DATA_DIR"
  systemctl start pranav-backend || true
  echo "Restored backup from $ARCHIVE"
else
  echo "Usage: $0 [backup | restore <archive.tar.gz>]"
  exit 1
fi
