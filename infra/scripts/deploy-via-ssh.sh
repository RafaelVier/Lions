#!/usr/bin/env bash
set -euo pipefail

[[ $# -eq 1 && ( "$1" == backend || "$1" == database ) ]] || { echo 'Usage: bash deploy-via-ssh.sh backend|database' >&2; exit 1; }
service=$1
for name in SSH_HOST SSH_USER SSH_KNOWN_HOSTS SSH_PRIVATE_KEY RUNNER_TEMP; do
  [[ -n "${!name:-}" ]] || { echo "::error::Configure $name before deploying."; exit 1; }
done
SSH_PORT=${SSH_PORT:-22}
DEPLOY_PATH=${DEPLOY_PATH:-/opt/lions/backend}
DATABASE_DEPLOY_PATH=${DATABASE_DEPLOY_PATH:-/opt/lions/database}
[[ "$SSH_HOST" =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]*$ ]] || { echo '::error::SSH_HOST must be a DNS name or IPv4 address.'; exit 1; }
[[ "$SSH_USER" =~ ^[a-zA-Z_][a-zA-Z0-9_-]*$ ]] || { echo '::error::Invalid SSH_USER.'; exit 1; }
[[ "$SSH_PORT" =~ ^[0-9]{1,5}$ ]] && (( 10#$SSH_PORT >= 1 && 10#$SSH_PORT <= 65535 )) || { echo '::error::Invalid SSH_PORT.'; exit 1; }
for path in "$DEPLOY_PATH" "$DATABASE_DEPLOY_PATH"; do
  [[ "$path" =~ ^(/[a-zA-Z0-9_-]+)+$ ]] || { echo '::error::Deployment paths must be absolute, using letters, numbers, underscores and hyphens.'; exit 1; }
done
[[ "$DEPLOY_PATH" != "$DATABASE_DEPLOY_PATH" ]] || { echo '::error::Backend and database must use different deployment directories.'; exit 1; }
if [[ "$service" == backend ]]; then
  [[ "${BACKEND_IMAGE:-}" =~ ^[a-z0-9._/-]+@sha256:[a-f0-9]{64}$ ]] || { echo '::error::Invalid published image digest.'; exit 1; }
  remote_path=$DEPLOY_PATH
  env_file=.env.prod
  files=(infra/backend/compose.yaml)
else
  remote_path=$DATABASE_DEPLOY_PATH
  env_file=.env.database
  files=(infra/database/compose.yaml infra/database/init-app-db.sh infra/database/backup.sh)
fi

umask 077
key_file="$RUNNER_TEMP/lions-deploy-key"
hosts_file="$RUNNER_TEMP/lions-known-hosts"
trap 'rm -f "$key_file" "$hosts_file"' EXIT
printf '%s\n' "$SSH_PRIVATE_KEY" > "$key_file"
printf '%s\n' "$SSH_KNOWN_HOSTS" > "$hosts_file"
ssh-keygen -y -P '' -f "$key_file" > /dev/null
options=(-i "$key_file" -o BatchMode=yes -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes -o "UserKnownHostsFile=$hosts_file" -o ConnectTimeout=15 -o ServerAliveInterval=15 -o ServerAliveCountMax=3)
target="$SSH_USER@$SSH_HOST"
ssh "${options[@]}" -p "$SSH_PORT" "$target" \
  "test -d '$remote_path' && test -f '$remote_path/$env_file'"
scp "${options[@]}" -P "$SSH_PORT" "${files[@]}" "$target:$remote_path/"
if [[ "$service" == backend ]]; then
  # A failed backup prevents starting the backend and applying Flyway migrations.
  ssh "${options[@]}" -p "$SSH_PORT" "$target" \
    "bash '$DATABASE_DEPLOY_PATH/backup.sh' '$DATABASE_DEPLOY_PATH' && bash -s -- '$remote_path' '$BACKEND_IMAGE'" < infra/backend/deploy.sh
else
  ssh "${options[@]}" -p "$SSH_PORT" "$target" \
    "bash -s -- '$remote_path'" < infra/database/deploy.sh
fi
