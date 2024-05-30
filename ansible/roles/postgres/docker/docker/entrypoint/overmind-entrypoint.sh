#!/bin/bash

# Env
# .bashrc to bring the connection environments
. /root/.bashrc

# We start as daemon to get all the logs in the same proc (ie proc 1)
command="${OVERMIND_ENV:-OVERMIND_CAN_DIE=sql_exporter,postgres_exporter OVERMIND_SHOW_TIMESTAMPS=1} overmind start"
printf "\nStarting the main process. Command:\n%s\n" "$command"
eval "$command"

# Log (process demonized output to stdout)
#command="overmind echo"
#printf "\nStarting overmind as daemon. Command:\n%s\n" "$command"

