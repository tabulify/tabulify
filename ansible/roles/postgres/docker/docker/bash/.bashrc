# ~/.bashrc: executed by bash(1) for non-login shells.

############################
# Docker/Conf env
# https://github.com/docker-library/docs/blob/master/postgres/README.md#environment-variables
############################
export POSTGRES_DB="${POSTGRES_DB:-$POSTGRES_USER}"

############################
# Default connection variable for postgres (psql, pg_dump, ...) and wal-g uses them also
# Pg Doc: https://www.postgresql.org/docs/current/libpq-envars.html
# Wal-g doc: https://github.com/wal-g/wal-g/blob/master/docs/PostgreSQL.md#configuration
############################
export PGHOST=/var/run/postgresql
export PGUSER="${POSTGRES_USER}"
export PGDATABASE="${POSTGRES_DB}"
# PGPASSWORD is not required to connect from localhost


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
