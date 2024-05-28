#!/bin/bash
set -e

## If not set, return
if [ -z "$PG_CRON_DB" ]; then
  echo "PG_CRON_DB env, not configured. PlPython Extension not enabled"
  exit;
fi;

echo "Enabling PL Python extension"
psql -v ON_ERROR_STOP=1 <<-EOSQL
	CREATE EXTENSION plpython3u;
EOSQL
