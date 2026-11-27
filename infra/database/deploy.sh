#!/usr/bin/env bash
set -euo pipefail

[[ $# -eq 1 && "$1" = /* ]] || { echo 'Usage: bash deploy.sh /absolute/database/path' >&2; exit 1; }
cd "$1"
[[ -f .env.database && -f compose.yaml && -f init-app-db.sh && -f backup.sh ]] || { echo 'Prepare the database directory and .env.database first.' >&2; exit 1; }
compose=(docker compose --env-file .env.database -f compose.yaml)
"${compose[@]}" config --quiet

# A pull failure leaves the running database untouched.
"${compose[@]}" pull postgres
existing=$("${compose[@]}" ps -a -q postgres)
if [[ -n "$existing" ]]; then
  # Abort instead of updating an existing database without a usable backup.
  bash backup.sh "$PWD"
fi
if ! docker volume inspect lions-postgres-data > /dev/null 2>&1; then
  docker volume create lions-postgres-data > /dev/null
fi
"${compose[@]}" up -d --no-deps --wait --wait-timeout 180 postgres
# Check the application login, not just pg_isready. Initialization only runs once.
"${compose[@]}" exec -T postgres sh -c \
  'export PGPASSWORD="$APP_DB_PASSWORD"; exec psql -h 127.0.0.1 -U "$APP_DB_USER" -d "$APP_DB_NAME" --set ON_ERROR_STOP=1 --command "SELECT 1"' > /dev/null
echo 'PostgreSQL is healthy and the application login works.'
