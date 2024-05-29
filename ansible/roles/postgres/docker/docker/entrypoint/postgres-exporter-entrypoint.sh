command="postgres_exporter ${POSTGRES_EXPORTER_FLAGS:---log.level=warn}"
printf "\nMetrics available at http://localhost:9187/metrics"
printf "\nExecuting the command:\n%s\n" "$command"
eval "$command"

