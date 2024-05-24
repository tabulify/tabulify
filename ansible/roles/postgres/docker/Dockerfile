
# version of this file
ARG IMAGE_VERSION=1.0.0
# PG version
ARG PG_MAJOR_VERSION=16
ARG PG_VERSION=16.3
# https://hub.docker.com/_/postgres
ARG PG_DOCKER_TAG=16.3
# Build Extensions
ARG PG_SAFEUPDATE_VERSION=1.5
ARG WAL_G_VERSION=3.0.0
# Supervisor Version
ARG OVERMIND_VERSION=2.2.1
# Postgres exporter
# https://github.com/prometheus-community/postgres_exporter/releases
ARG PG_EXPORTER_VERSION=0.15.0

# C Extensions use pgxs for the build
# https://www.postgresql.org/docs/current/extend-pgxs.html
FROM postgres:${PG_DOCKER_TAG} as pgxs_builder
ARG PG_MAJOR_VERSION
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    checkinstall \
    cmake \
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


# Use debian:bookworm-slim as the base image
# as this is the base image of postgres
# https://github.com/wal-g/wal-g?tab=readme-ov-file#installing
# run it
# docker run --env-file secret.env --rm -it debianslim_wal-g
FROM debian:bookworm-slim as wal-g

# Set environment variables
ARG WAL_G_VERSION
# https://github.com/wal-g/wal-g?tab=readme-ov-file#compression
ENV WALG_COMPRESSION_METHOD brotli
ENV WALG_LIBSODIUM_KEY_TRANSFORM base64

# Install dependencies
RUN apt-get update && apt-get install -y \
    git \
    golang-go \
    libbrotli-dev \
    liblzo2-dev \
    libsodium-dev \
    cmake \
    && apt-get clean

# Set Go environment variables
ENV GOPATH=/go
ENV PATH=$GOPATH/bin:/usr/local/go/bin:$PATH

# Clone the WAL-G repository and checkout the specified version
RUN mkdir -p /go/src/github.com/wal-g/wal-g &&\
    git clone --branch v${WAL_G_VERSION} --single-branch https://github.com/wal-g/wal-g /go/src/github.com/wal-g/wal-g && \
    cd /go/src/github.com/wal-g/wal-g && \
    export USE_BROTLI=1 && \
    export USE_LIBSODIUM=1 && \
    export USE_LZO=1 && \
    make deps && \
    make pg_build && \
    cp main/pg/wal-g /usr/local/bin/wal-g && \
    chmod +x /usr/local/bin/wal-g

ENTRYPOINT ["wal-g"]


FROM postgres:${PG_DOCKER_TAG} as final
ENV PGDATA=/data/postgresql
ENV PGPASSFILE=/data/.pgpass
ARG IMAGE_VERSION
ARG OVERMIND_VERSION
ARG PG_EXPORTER_VERSION
ARG PG_VERSION

# Ensure UTF-8
ENV LANG=en_US.UTF-8
ENV LC_CTYPE=C.UTF-8
ENV LC_COLLATE=C.UTF-8

# Deb Package Extension
COPY --from=pg-safeupdate /tmp/*.deb /tmp

# Installation with PG Package Extension )
RUN apt-get update && apt-get install --no-install-recommends -y \
    postgresql-${PG_MAJOR}-cron \
    postgresql-${PG_MAJOR}-pgvector \
    bash \
    curl \
    /tmp/*.deb \
    # Needed for anything using libcurl
    # https://github.com/supabase/postgres/issues/573
    ca-certificates \
    && rm -rf /var/lib/apt/lists/* /tmp/* \
    && apt-get clean


LABEL image-version=${IMAGE_VERSION}
LABEL pg-version=${PG_VERSION}
LABEL pg-exporter-version=${PG_EXPORTER_VERSION}
LABEL OVERMIND_VERSION=${OVERMIND_VERSION}

###############################################
# WAL archiving
# https://www.postgresql.org/docs/current/continuous-archiving.html#BACKUP-ARCHIVING-WAL
############################################################################################
ARG ARCHIVE_MODE=on
# When archive_mode is enabled (on or always),
# completed WAL segments are sent to archive storage by setting archive_command or archive_library.
# Off by default to avoid crushing PostgreSQL production data by mistake

ARG ARCHIVE_TIMEOUT=3600
# Archive timeout
# If the value is specified without units, it is taken as seconds.
# The archive_command is only invoked for completed WAL segments.
# With low traffic, the command would never be executed.
# Archived files that are closed early due to a forced switch
# are still the same length as completely full files.
# Therefore, it is unwise to use a very short archive_timeout — it will bloat your archive storage.
# Use Streaming replication, instead of archiving, if you want data to be copied off the primary server more quickly than that.
# https://www.postgresql.org/docs/current/runtime-config-wal.html#GUC-ARCHIVE-TIMEOUT

# archive/restore command
# restore command
# https://www.postgresql.org/docs/current/runtime-config-wal.html#GUC-RESTORE-COMMAND
# logging_collector should be on (default?)
RUN echo 'Postgres Configuration File' \
    && mkdir -p /etc/postgresql/ \
    && cp /usr/local/share/postgresql/postgresql.conf.sample /etc/postgresql/postgresql.conf \
    && echo "archive_mode = ${ARCHIVE_MODE}" >> /etc/postgresql/postgresql.conf \
    && echo "archive_timeout = ${ARCHIVE_TIMEOUT}" >> /etc/postgresql/postgresql.conf \
    && echo "archive_command = 'wal-g wal-push %p'" >> /etc/postgresql/postgresql.conf \
    && echo "restore_command = 'wal-g wal-fetch %f %p'" >> /etc/postgresql/postgresql.conf \
    && echo "cron.database_name = 'postgres'" >> /etc/postgresql/postgresql.conf

###################################
# Walg
####################################
COPY --from=wal-g /usr/local/bin/wal-g /usr/local/bin/wal-g


# See environment variables documentation \
# https://github.com/wal-g/wal-g/blob/master/docs/STORAGES.md
#ENV WALE_S3_PREFIX=s3://bucket-name/path/to/folder
#ENV AWS_ACCESS_KEY_ID=xxxx
#ENV AWS_SECRET_ACCESS_KEY=secret
#ENV AWS_ENDPOINT=s3-like-service:9000
ENV WALG_COMPRESSION_METHOD=brotli

# See environment variables documentation https://github.com/wal-g/wal-g/blob/master/PostgreSQL.md#configuration
ENV PGHOST=/var/run/postgresql
ENV PGUSER=postgres
#ENV PGPASSWORD=secret
#ENV WALG_PGP_KEY_PATH=/keys/wal-g.pub

# Prometheus exporter
# https://github.com/prometheus-community/postgres_exporter
RUN curl -L https://github.com/prometheus-community/postgres_exporter/releases/download/v${PG_EXPORTER_VERSION}/postgres_exporter-${PG_EXPORTER_VERSION}.linux-amd64.tar.gz -o postgres_exporter.gz && \
    gunzip postgres_exporter.gz && \
    chmod +x postgres_exporter && \
    mv postgres_exporter /usr/local/bin/ \

## Overmind
RUN curl -L "https://github.com/DarthSim/overmind/releases/download/v${OVERMIND_VERSION}/overmind-v${OVERMIND_VERSION}-linux-amd64.gz" -o overmind.gz && \
    gunzip overmind.gz && \
    chmod +x overmind && \
    mv overmind /usr/local/bin/

WORKDIR /app
# Procfile should be located in the working directory (or -f path/to/your/Procfile)
ADD Procfile /app/
CMD ["overmind", "start"]
