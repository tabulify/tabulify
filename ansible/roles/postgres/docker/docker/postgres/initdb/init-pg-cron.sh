#!/bin/bash
set -e

## If not set, return
if [ -z "$PG_CRON_DB" ]; then
  echo "PG_CRON_DB env, not configured. Extension not enabled"
  exit;
fi;

echo "Enabling Pg_cron extension (PG_CRON_DB env configured to $PG_CRON_DB"
psql -v ON_ERROR_STOP=1 <<-EOSQL
	CREATE EXTENSION pg_cron;
EOSQL
