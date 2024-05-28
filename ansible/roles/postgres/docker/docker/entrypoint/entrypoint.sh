#!/bin/bash

# Because we use overmind
# we replace the custom docker entrypoint
# https://github.com/docker-library/postgres/blob/d08757ccb56ee047efd76c41dbc148e2e2c4f68f/16/bookworm/docker-entrypoint.sh
# The docker-entrypoint is called postgres-entrypoint

# Start the passed command ($*)
/bin/bash -c "$*"
