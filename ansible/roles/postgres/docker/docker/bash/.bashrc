# ~/.bashrc: executed by bash(1) for non-login shells.

############################
# Docker/Conf env
# https://github.com/docker-library/docs/blob/master/postgres/README.md#environment-variables
############################
# postgres is the default database, is always present
# and is the default of all extensions as stated here
# https://www.postgresql.org/docs/9.1/creating-cluster.html
export POSTGRES_DB="${POSTGRES_DB:-postgres}"
export POSTGRES_USER="${POSTGRES_USER:-${POSTGRES_DB}}"

############################
# Default connection variable for postgres (psql, pg_dump, ...) and wal-g uses them also
# Pg Doc: https://www.postgresql.org/docs/current/libpq-envars.html
# Wal-g doc: https://github.com/wal-g/wal-g/blob/master/docs/PostgreSQL.md#configuration
############################
export PGHOST=/var/run/postgresql
export PGUSER="${POSTGRES_USER}"
export PGDATABASE="${POSTGRES_DB}"
# PGPASSWORD is not required to connect from localhost

# color in diagnostic messages (https://www.postgresql.org/docs/current/app-pgrestore.html)
# value may be always, auto and never
export PG_COLOR=always


export LS_OPTIONS='--color=auto'
# eval "$(dircolors)"
alias ls='ls $LS_OPTIONS'
alias ll='ls $LS_OPTIONS -l'
alias l='ls $LS_OPTIONS -lA'

# Some more alias to avoid making mistakes:
# alias rm='rm -i'
# alias cp='cp -i'
# alias mv='mv -i'

function echo_err() {
  RED='\033[0;31m'
  NC='\033[0m' # No Color
  echo -e "${RED}$1${NC}" >&2
}
export -f echo_err
