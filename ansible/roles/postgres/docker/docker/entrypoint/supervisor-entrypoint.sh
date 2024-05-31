#!/bin/bash

# Env
# .bashrc to bring the connection environments
. /root/.bashrc


# we set the `c` to avoid the below warning:
# UserWarning: Supervisord is running as root and it is searching
# for its configuration file in default locations (including its current working directory);
# you probably want to specify a "-c" argument specifying
# an absolute path to a configuration file for improved security.
supervisord -c /supervisord.conf
