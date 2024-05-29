#!/bin/bash

# https://github.com/burningalchemist/sql_exporter
# Linked from postgres_exporter

# Copy the configuration file to the data directory
source_dir="/sql_exporter"
target_dir="$DATA_HOME/sql_exporter"
sql_exporter_path="${target_dir}/sql_exporter.yml"

if [ ! -f "$sql_exporter_path" ]; then
    mkdir -p "$target_dir"
    cp -rf "$source_dir"/* "$target_dir"
fi



command="sql_exporter -config.file ${sql_exporter_path} ${SQL_EXPORTER_FLAGS}"
printf "\nMetrics available at http://localhost:9399/metrics"
printf "\nExecuting the command:\n%s\n" "$command"
eval "$command"

