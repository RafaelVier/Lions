#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo 'Usage: bash deploy.sh /absolute/deploy/path repository@sha256:digest' >&2
  exit 1
fi

deploy_path=$1
export BACKEND_IMAGE=$2
[[ "$deploy_path" = /* && -d "$deploy_path" ]] || { echo 'Deployment directory does not exist.' >&2; exit 1; }
[[ "$BACKEND_IMAGE" =~ ^[a-z0-9._/-]+@sha256:[a-f0-9]{64}$ ]] || { echo 'Invalid image digest.' >&2; exit 1; }
cd "$deploy_path"
[[ -f .env.prod && -f compose.yaml ]] || { echo 'Prepare .env.prod and compose.yaml on the server first.' >&2; exit 1; }

compose=(docker compose --env-file .env.prod -f compose.yaml)
"${compose[@]}" config --quiet
# Pull before replacing the running container. Registry credentials live on the server.
"${compose[@]}" pull backend
if ! "${compose[@]}" up -d --no-deps --wait --wait-timeout 240 backend; then
  echo 'Backend failed its healthcheck. Inspect docker compose logs on the server; no automatic rollback was performed.' >&2
  exit 1
fi

# Record only a healthy release, atomically, for maintenance commands on the server.
umask 077
release_file=$(mktemp .release.env.XXXXXX)
trap 'rm -f "$release_file"' EXIT
printf 'BACKEND_IMAGE=%s\n' "$BACKEND_IMAGE" > "$release_file"
mv "$release_file" .release.env
echo 'Backend is healthy; release recorded in .release.env.'
