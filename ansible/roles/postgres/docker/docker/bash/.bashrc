# ~/.bashrc: executed by bash(1) for non-login shells.

############################
# Default connection variable for postgres (psql, pg_dump, ...) and wal-g uses them also
# https://www.postgresql.org/docs/current/libpq-envars.html
# https://github.com/wal-g/wal-g/blob/master/docs/PostgreSQL.md#configuration
# we use the Docker env
# https://github.com/docker-library/docs/blob/master/postgres/README.md#environment-variables
export PGHOST=/var/run/postgresql
export PGUSER="${POSTGRES_USER:-postgres}"
# POSTGRES_DB default to POSTGRES_USER value
# POSTGRES_PASSWORD is not required to connect from localhost


export LS_OPTIONS='--color=auto'
# eval "$(dircolors)"
alias ls='ls $LS_OPTIONS'
alias ll='ls $LS_OPTIONS -l'
alias l='ls $LS_OPTIONS -lA'

# Some more alias to avoid making mistakes:
# alias rm='rm -i'
# alias cp='cp -i'
# alias mv='mv -i'
