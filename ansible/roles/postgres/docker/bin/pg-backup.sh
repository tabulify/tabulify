#!/bin/bash

pg_dump postgres > $PGDUMPDATA/dumpfile.sql
restic backup $PGDUMPDATA
