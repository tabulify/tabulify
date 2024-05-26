#!/bin/bash

# A wrapper around the docker entrypoint to recover
# https://github.com/docker-library/postgres/blob/d08757ccb56ee047efd76c41dbc148e2e2c4f68f/16/bookworm/docker-entrypoint.sh#L161
RECOVERY_SIGNAL_PATH=$PGDATA/recovery.signal

if [ -f "$RECOVERY_SIGNAL_PATH" ]; then
    echo "Recovering file sign found ($RECOVERY_SIGNAL_PATH)"
    echo "Deleting the actual pgdata directory"
    rm -rf "${PGDATA:?}"/* || exit 1
    echo "Fetching the latest backup"
    wal-g backup-fetch "$PGDATA" LATEST || exit 1
    echo "Recreating the recovery signal file"
    touch "$RECOVERY_SIGNAL_PATH" || exit 1
fi

# Start the passed command ($*)
/bin/sh -c "$*"
