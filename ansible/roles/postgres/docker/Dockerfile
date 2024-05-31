
# version of this file
ARG IMAGE_VERSION=1.0.0
# PG version
ARG PG_MAJOR_VERSION=16
ARG PG_VERSION=16.3
# https://hub.docker.com/_/postgres
# https://github.com/docker-library/postgres/blob/d08757ccb56ee047efd76c41dbc148e2e2c4f68f/16/bookworm/Dockerfile
ARG PG_DOCKER_TAG=16.3
# Build Extensions
ARG PG_SAFEUPDATE_VERSION=1.5
ARG WALG_VERSION=3.0.0
# Supervisor Version
# https://github.com/DarthSim/overmind/releases
ARG OVERMIND_VERSION=2.5.1
# Postgres exporter
# https://github.com/prometheus-community/postgres_exporter/releases
ARG PG_EXPORTER_VERSION=0.15.0
# https://github.com/restic/restic/releases/
ARG RESTIC_VERSION=0.16.4
# https://github.com/burningalchemist/sql_exporter/releases/
ARG SQL_EXPORTER_VERSION=0.14.3

# C Extensions use pgxs for the build
# https://www.postgresql.org/docs/current/extend-pgxs.html
FROM postgres:${PG_DOCKER_TAG} as pgxs_builder
ARG PG_MAJOR_VERSION
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    checkinstall \
    cmake \
    git \
    curl \
    # Needed for curl, libcurl (used in make file)
    ca-certificates \
    postgresql-server-dev-${PG_MAJOR_VERSION}

####################
# pg-safeupdate deb package creation
####################
FROM pgxs_builder as pg-safeupdate
ARG PG_SAFEUPDATE_VERSION
ADD "https://github.com/eradman/pg-safeupdate/archive/refs/tags/${PG_SAFEUPDATE_VERSION}.tar.gz" \
    /tmp/pg-safeupdate.tar.gz
RUN tar -xvf /tmp/pg-safeupdate.tar.gz -C /tmp \
    && rm -rf /tmp/pg-safeupdate.tar.gz
# Build from source
WORKDIR /tmp/pg-safeupdate-${PG_SAFEUPDATE_VERSION}
ENV USE_PGXS=1
RUN make -j$(nproc)
# Create debian package
RUN checkinstall -D --install=no --fstrans=no --backup=no --pakdir=/tmp --nodoc


###########################################
# Wal-g building
# https://github.com/wal-g/wal-g?tab=readme-ov-file#installing
# run it
# docker run --env-file secret.env --rm -it debianslim_wal-g
FROM pgxs_builder as wal-g

# Set environment variables
ARG WALG_VERSION
# https://github.com/wal-g/wal-g?tab=readme-ov-file#compression
ENV WALG_COMPRESSION_METHOD brotli
ENV WALG_LIBSODIUM_KEY_TRANSFORM base64

# Install dependencies
RUN apt-get update && apt-get install -y \
    golang-go \
    libbrotli-dev \
    liblzo2-dev \
    libsodium-dev \
    && apt-get clean

# Set Go environment variables
ENV GOPATH=/go
ENV PATH=$GOPATH/bin:/usr/local/go/bin:$PATH

# Clone the WAL-G repository and checkout the specified version
RUN mkdir -p /go/src/github.com/wal-g/wal-g &&\
    git clone --branch v${WALG_VERSION} --single-branch https://github.com/wal-g/wal-g /go/src/github.com/wal-g/wal-g && \
    cd /go/src/github.com/wal-g/wal-g && \
    export USE_BROTLI=1 && \
    export USE_LIBSODIUM=1 && \
    export USE_LZO=1 && \
    make deps && \
    make pg_build && \
    cp main/pg/wal-g /usr/local/bin/wal-g && \
    chmod +x /usr/local/bin/wal-g

ENTRYPOINT ["wal-g"]

###############################################
# Postgres Final
###############################################
FROM postgres:${PG_DOCKER_TAG} as final

ARG IMAGE_VERSION
ARG OVERMIND_VERSION
ARG PG_EXPORTER_VERSION
ARG PG_VERSION
ARG PG_MAJOR_VERSION
ARG RESTIC_VERSION
ARG WALG_VERSION
ARG SQL_EXPORTER_VERSION


LABEL image-version=${IMAGE_VERSION}
LABEL walg-version=${WALG_VERSION}
LABEL pg-version=${PG_VERSION}
LABEL pg-exporter-version=${PG_EXPORTER_VERSION}
LABEL overmind-version=${OVERMIND_VERSION}
LABEL pg-cron-version=${PG_MAJOR_VERSION}
LABEL pg-vector-version=${PG_MAJOR_VERSION}
LABEL sql-exporter-version=${SQL_EXPORTER_VERSION}

##############
# Env
###############
## The mounted directory
ENV DATA_HOME=/data

# Ensure UTF-8
ENV LANG=en_US.UTF-8
ENV LC_CTYPE=C.UTF-8
ENV LC_COLLATE=C.UTF-8
# TZ is the OS env
# Gnu https://www.gnu.org/software/libc/manual/html_node/TZ-Variable.html
ENV TZ 'Etc/UTC'

# Postgres Local Connection env
# so that when logged in, we can connect automatically and wal-g also
ADD docker/bash/.bashrc /root/.bashrc

##############
# Common Package
###############
RUN apt-get update && apt-get install --no-install-recommends -y \
    bash \
    curl \
    ca-certificates \
    unzip \
    bzip2 \
    # Templating with envsubst
    gettext

###############################################
# WAL-g, WAL archiving
# See postgres-wal-archiving.md
############################################################################################
COPY --from=wal-g /usr/local/bin/wal-g /usr/local/bin/wal-g
ARG ARCHIVE_MODE=on
ARG ARCHIVE_TIMEOUT=3600

####################################
# Prometheus exporter
# https://github.com/prometheus-community/postgres_exporter
####################################
RUN curl -L https://github.com/prometheus-community/postgres_exporter/releases/download/v${PG_EXPORTER_VERSION}/postgres_exporter-${PG_EXPORTER_VERSION}.linux-amd64.tar.gz -o postgres_exporter.tar.gz && \
    tar -xzvf postgres_exporter.tar.gz --strip-components=1 --no-anchored postgres_exporter && \
    rm postgres_exporter.tar.gz && \
    chmod +x postgres_exporter && \
    mv postgres_exporter /usr/local/bin/

# The postgres_exporter port
EXPOSE 9187
# The entrypoint
ADD docker/entrypoint/postgres-exporter-entrypoint.sh /usr/local/bin
RUN chmod +x /usr/local/bin/postgres-exporter-entrypoint.sh

####################################
# Sql exporter
# https://github.com/burningalchemist/sql_exporter
# Why? Linked from postgres_exporter
####################################
ADD "https://github.com/burningalchemist/sql_exporter/releases/download/${SQL_EXPORTER_VERSION}/sql_exporter-${SQL_EXPORTER_VERSION}.linux-amd64.tar.gz" \
    /tmp/sql_exporter.tar.gz
RUN tar -xzvf /tmp/sql_exporter.tar.gz --strip-components=1 --no-anchored sql_exporter && \
    rm /tmp/sql_exporter.tar.gz && \
    chmod +x sql_exporter && \
    mv sql_exporter /usr/local/bin/ && \
    mkdir -p /var/log/sql-exporter/

EXPOSE 9399
# The entrypoint
ADD docker/entrypoint/sql-exporter-entrypoint.sh /usr/local/bin
RUN chmod +x /usr/local/bin/sql-exporter-entrypoint.sh
# The conf (conf is copied in the data directory at start)
# the end / in the target is important to tell that this is a directory
ADD docker/sql_exporter/* /sql_exporter/

#####################################
# Restic
#####################################
RUN curl -L https://github.com/restic/restic/releases/download/v${RESTIC_VERSION}/restic_${RESTIC_VERSION}_linux_amd64.bz2 -o restic.bz2 && \
    bzip2 -d restic.bz2 && \
    chmod +x restic && \
    mv restic /usr/local/bin/ && \
    mkdir -p /etc/bash_completion.d && \
    restic generate --bash-completion /etc/bash_completion.d/restic

##################################
# Dbctl
##################################
# The log file should be writable by root and the user running postgres
ENV DBCTL_HOME=${DATA_HOME}/dbctl
ENV DBCTL_LOG_FILE=${DBCTL_HOME}/dbctl.log
ADD docker/bin/dbctl /usr/local/bin/
RUN chmod +x /usr/local/bin/dbctl && \
    mkdir -p "$DBCTL_HOME" && \
    touch "$DBCTL_LOG_FILE" && \
    chmod a+w "$DBCTL_LOG_FILE"

# The directory where to dump pg dump backup
# The os user is always postgres
ENV PG_DUMP_DATA=${DATA_HOME}/pgdump
RUN mkdir -p ${PG_DUMP_DATA} && \
    chown -R postgres ${PG_DUMP_DATA}

#############################
## Docker Entrypoint
############################
ADD docker/entrypoint/entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/entrypoint.sh # basic entrypoint
ENTRYPOINT ["entrypoint.sh"]

#############################
## Postgres
#############################
## wrapper around docker-entrypoint.sh
ADD docker/entrypoint/postgres-entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/postgres-entrypoint.sh

# All date are UTC (Os, Database, ...)
# https://www.postgresql.org/docs/current/libpq-envars.html
# Etc means etcetera
ENV PGTZ 'Etc/UTC'

# Conf
# https://github.com/docker-library/docs/blob/master/postgres/README.md#database-configuration
# postgresql.conf is a template and accepts env so that we can be consistent
# Don't add secret in their
ADD docker/postgres/conf/postgresql.conf /etc/postgresql/postgresql.conf.template
RUN envsubst < /etc/postgresql/postgresql.conf.template > /etc/postgresql/postgresql.conf

# PgData is not set in /var/lib/postgresql/data
# to be able to mount only one volume
# if more saved data are needed
ENV PGDATA=${DATA_HOME}/pgdata

# Database Init Scripts
# The (*.sql, *.sql.gz, or *.sh) init scripts
# Executed
#  * in sorted name order as defined by the current locale (default to en_US.utf8)
#  * at database initialization only happens on container startup
#  * ie the data directory is empty
# https://github.com/docker-library/postgres/blob/d08757ccb56ee047efd76c41dbc148e2e2c4f68f/16/bookworm/docker-entrypoint.sh#L161
ADD docker/postgres/initdb/* /docker-entrypoint-initdb.d

# Postgres Scripts
# the end / in the target is important to tell that this is a directory
ADD docker/postgres/script/* /script/

# pg-safeupdate extension
COPY --from=pg-safeupdate /tmp/*.deb /tmp
RUN apt-get install --no-install-recommends -y /tmp/*.deb

# cron, pgvector, plpython extension
RUN apt-get install --no-install-recommends -y \
    postgresql-${PG_MAJOR}-cron \
    postgresql-${PG_MAJOR}-pgvector \
    python3 \
    postgresql-plpython3-${PG_MAJOR}

####################################
# Supervisor
####################################
# wrapper around overmind to allow customization
ADD supervisord.conf .
ADD docker/entrypoint/supervisor-entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/supervisor-entrypoint.sh && \
    apt-get install --no-install-recommends -y supervisor

EXPOSE 9001

################################# \
# Clean
#################################
RUN rm -rf /var/lib/apt/lists/* /tmp/* \
    # No apt-get can be used from this point or you need to run apt-get update \
    # to update the repo again
    && apt-get clean

# CMD
CMD ["supervisor-entrypoint.sh"]
