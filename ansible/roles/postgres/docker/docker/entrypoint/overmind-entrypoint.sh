#!/bin/bash



# Env
# .bashrc to bring the connection environments
. /root/.bashrc

command="${OVERMIND_ENV:-OVERMIND_CAN_DIE=sql_exporter,postgres_exporter OVERMIND_SHOW_TIMESTAMPS=1} overmind start"
printf "\nExecuting the command:\n%s\n" "$command"
eval "$command"
