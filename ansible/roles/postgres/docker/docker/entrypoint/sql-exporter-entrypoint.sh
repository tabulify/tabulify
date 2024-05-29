#!/bin/bash

# https://github.com/burningalchemist/sql_exporter
# Linked from postgres_exporter

# Copy the configuration file to the data directory
source_dir="/sql-exporter"
target_dir="$DATA_HOME/sql-exporter"

if [ ! -d "$target_dir" ]; then
    mkdir -p "$target_dir"
    cp -r "$source_dir"/* "$target_dir"
fi



command="sql_exporter -config.file ${target_dir}/sql_exporter.yml ${SQL_EXPORTER_FLAGS}"
printf "\nMetrics available at http://localhost:9399/metrics"
printf "\nExecuting the command:\n%s\n" "$command"
eval "$command"

