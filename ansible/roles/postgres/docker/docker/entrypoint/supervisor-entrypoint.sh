#!/bin/bash

# Env
# .bashrc to bring the connection environments
. /root/.bashrc

SUPERVISOR_CONF_PATH=${SUPERVISOR_CONF_PATH:-/supervisord.conf}

LOG_HOME=${LOG_HOME:-/var/log}

# Create log dirs
# The log directory are not created and just stop supervisor
# https://github.com/Supervisor/supervisor/issues/120#issuecomment-209292870
SQL_EXPORTER_LOG=${LOG_HOME}/sql-exporter/sql-exporter.log
POSTGRES_EXPORTER_LOG=${LOG_HOME}/postgres-exporter/postgres-exporter.log
POSTGRES_LOG=${LOG_HOME}/postgres/postgres.log
LOGS=(
    "$SQL_EXPORTER_LOG"
    "$POSTGRES_EXPORTER_LOG"
    "$POSTGRES_LOG"
)
for LOG in "${LOGS[@]}"; do
    LOG_PARENT=$(dirname "$LOG")
    mkdir -p "$LOG_PARENT"
    touch "$LOG"
done


# we set the `c` to avoid the below warning:
# UserWarning: Supervisord is running as root and it is searching
# for its configuration file in default locations (including its current working directory);
# you probably want to specify a "-c" argument specifying
# an absolute path to a configuration file for improved security.
supervisord -c "$SUPERVISOR_CONF_PATH"
