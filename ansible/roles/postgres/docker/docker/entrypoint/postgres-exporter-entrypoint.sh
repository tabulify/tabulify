#!/bin/bash

# Env
# .bashrc to bring the connection environments
. /root/.bashrc

command="${POSTGRES_EXPORTER_ENV:-DATA_SOURCE_NAME='sslmode=disable' PG_EXPORTER_DISABLE_SETTINGS_METRICS=true} postgres_exporter ${POSTGRES_EXPORTER_FLAGS:---log.level=warn}"
printf "\nMetrics available at http://localhost:9187/metrics"
printf "\nExecuting the command:\n%s\n" "$command"
eval "$command"

