#!/bin/bash
set -e

## If not set, return
if [ -z "$PG_CRON_DB" ]; then
  echo "PG_CRON_DB env, not configured. Pg Cron Extension not enabled"
  exit;
fi;

echo "Enabling Pg_cron and PlPython extension (PG_CRON_DB env configured to $PG_CRON_DB"
psql -v ON_ERROR_STOP=1 <<-EOSQL
	CREATE EXTENSION pg_cron;
	-- plpython3u because it's need to start OS script
	CREATE EXTENSION plpython3u;
EOSQL

echo "Creating the dbctl procedure"
psql -v ON_ERROR_STOP=1 -f /script/dbctl
