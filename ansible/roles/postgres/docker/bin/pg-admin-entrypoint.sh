#!/bin/bash

# A wrapper around the docker entrypoint to recover
# https://github.com/docker-library/postgres/blob/d08757ccb56ee047efd76c41dbc148e2e2c4f68f/16/bookworm/docker-entrypoint.sh#L161
RECOVERY_SIGNAL_PATH=$PGDATA/recovery.signal

if [ -f "$RECOVERY_SIGNAL_PATH" ]; then
    echo "Recovering file signal found ($RECOVERY_SIGNAL_PATH)"
    echo "Deleting the actual pgdata directory"
    rm -rf "${PGDATA:?}"/* || exit 1
    echo "Fetching the latest backup"
    wal-g backup-fetch "$PGDATA" LATEST || exit 1
    echo "Recreating the recovery signal file"
    touch "$RECOVERY_SIGNAL_PATH" || exit 1
else
    echo "No Recovering file signal found ($RECOVERY_SIGNAL_PATH)"
fi

## Restic
## Only if the repo is set
if [[ -n ${RESTIC_REPOSITORY} ]]; then
    if [[ -n ${RESTIC_PASSWORD+x} ]]; then
      if  ! restic snapshots > /dev/null; then
              echo "Restic Repo not found - Restic init at ${RESTIC_REPOSITORY}"
              restic init
              echo "Done at ${RESTIC_REPOSITORY}"
      else
        echo "RESTIC Repo already configured";
      fi
    else
      echo "RESTIC_PASSWORD is not set";
      exit 1;
    fi
else
  echo "No restic repo configured - Ignoring"
fi

# Start the passed command ($*)
/bin/sh -c "$*"
