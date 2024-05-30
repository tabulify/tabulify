#!/bin/bash

# Env
# .bashrc to bring the connection environments
. /root/.bashrc

# search supervisord.conf
# in the current directory
# or can be passed via `-c /path/to/supervisord.conf`

supervisord
