#!/usr/bin/env bash
set -euo pipefail

# Executado pela imagem oficial somente ao inicializar um volume vazio.
: "${APP_DB_NAME:?Informe APP_DB_NAME}"
: "${APP_DB_USER:?Informe APP_DB_USER}"
: "${APP_DB_PASSWORD:?Informe APP_DB_PASSWORD}"
[[ "$APP_DB_NAME" =~ ^[a-z][a-z0-9_]{0,62}$ && "$APP_DB_NAME" != postgres && "$APP_DB_NAME" != template* ]] || { echo 'Invalid application database name.' >&2; exit 1; }
[[ "$APP_DB_USER" =~ ^[a-z][a-z0-9_]{0,62}$ && "$APP_DB_USER" != postgres && "$APP_DB_USER" != pg_* ]] || { echo 'Invalid application role name.' >&2; exit 1; }

psql --username postgres --dbname postgres --set ON_ERROR_STOP=1 \
  --set app_database="$APP_DB_NAME" --set app_user="$APP_DB_USER" --set app_password="$APP_DB_PASSWORD" <<'SQL'
CREATE ROLE :"app_user" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD :'app_password';
CREATE DATABASE :"app_database" OWNER :"app_user";
REVOKE ALL ON DATABASE :"app_database" FROM PUBLIC;
\connect :app_database
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
SQL
