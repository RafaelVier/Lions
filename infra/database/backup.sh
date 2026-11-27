#!/usr/bin/env bash
set -euo pipefail

[[ $# -eq 1 && "$1" = /* ]] || { echo 'Usage: bash backup.sh /absolute/database/path' >&2; exit 1; }
cd "$1"
[[ -f .env.database && -f compose.yaml ]] || { echo 'Database configuration is missing.' >&2; exit 1; }
umask 077
mkdir -p backups
backup_file=$(mktemp "backups/$(date -u +%Y%m%dT%H%M%SZ)-XXXXXX.dump")
trap 'rm -f "$backup_file"' EXIT
docker compose --env-file .env.database -f compose.yaml exec -T postgres \
  sh -c 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec pg_dump -h 127.0.0.1 -U postgres -d "$APP_DB_NAME" --format=custom' > "$backup_file"
[[ -s "$backup_file" ]] || { echo 'Database backup is empty.' >&2; exit 1; }
trap - EXIT
echo "Database backup saved: $PWD/$backup_file"
