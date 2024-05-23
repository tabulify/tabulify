# https://hub.docker.com/_/postgres
ARG PG_VERSION=16.3
ARG PG_DOCKER_TAG=16.3
ARG PG_MAJOR_VERSION=16

# Use debian:bookworm-slim as the base image
# as this is the base image of postgres
# https://github.com/wal-g/wal-g?tab=readme-ov-file#installing
# run it
# docker run --env-file secret.env --rm -it debianslim_wal-g
FROM postgres:${PG_DOCKER_TAG} as wal-g

# Set environment variables
ENV WAL_G_VERSION v3.0.0
# https://github.com/wal-g/wal-g?tab=readme-ov-file#compression
ENV WALG_COMPRESSION_METHOD brotli
ENV WALG_LIBSODIUM_KEY_TRANSFORM base64

# Install dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
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
    git clone --branch ${WAL_G_VERSION} --single-branch https://github.com/wal-g/wal-g /go/src/github.com/wal-g/wal-g && \
    cd /go/src/github.com/wal-g/wal-g && \
    export USE_BROTLI=1 && \
    export USE_LIBSODIUM=1 && \
    export USE_LZO=1 && \
    make deps && \
    make pg_build && \
    cp /go/src/github.com/wal-g/wal-g/main/pg/wal-g /usr/local/bin/wal-g && \
    chmod +x /usr/local/bin/wal-g

ENTRYPOINT ["wal-g"]

# Postgres Exporter
# https://github.com/prometheus-community/postgres_exporter
FROM wrouesnel/postgres_exporter:latest AS postgres_exporter

FROM postgres:${PG_DOCKER_TAG} as final
ENV PGDATA=/data/postgresql
ENV PGPASSFILE=/data/.pgpass
ARG VERSION=1.0.0
ENV OVERMIND_VERSION 2.2.1


# Package (PostGIS, ...)
RUN apt-get update && apt-get install --no-install-recommends -y \
    postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR \
    postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR-scripts \
    bash \
    curl \
    && apt-get clean



LABEL version=${VERSION}
LABEL pg-version=${PG_VERSION}

###################################
# Walg
####################################
COPY --from=wal-g /usr/local/bin/wal-g /usr/local/bin/wal-g

ARG ARCHIVE_MODE=on
# When archive_mode is enabled (on or always),
# completed WAL segments are sent to archive storage by setting archive_command or archive_library.
# Off by default to avoid crushing PostgreSQL production data by mistake

ARG ARCHIVE_TIMEOUT=60
# Archive timeout
# The archive_command is only invoked for completed WAL segments.
# With low traffic, the command would never be executed.
# Archived files that are closed early due to a forced switch
# are still the same length as completely full files.
# Therefore, it is unwise to use a very short archive_timeout — it will bloat your archive storage.
# Use Streaming replication, instead of archiving, if you want data to be copied off the primary server more quickly than that.
# https://www.postgresql.org/docs/current/runtime-config-wal.html#GUC-ARCHIVE-TIMEOUT

# enable the archive mode with archive command
RUN mkdir -p /etc/postgresql/ \
    && cp /usr/local/share/postgresql/postgresql.conf.sample /etc/postgresql/postgresql.conf \
    && echo "archive_mode = ${ARCHIVE_MODE}" >> /etc/postgresql/postgresql.conf \
    && echo "archive_timeout = ${ARCHIVE_TIMEOUT}" >> /etc/postgresql/postgresql.conf \
    && echo "archive_command = 'wal-g wal-push %p'" >> /etc/postgresql/postgresql.conf \
    && echo "restore_command = 'wal-g wal-fetch %f %p'" >> /etc/postgresql/postgresql.conf


# See environment variables documentation \
# https://github.com/wal-g/wal-g/blob/master/docs/STORAGES.md
#ENV WALE_S3_PREFIX=s3://bucket-name/path/to/folder
#ENV AWS_ACCESS_KEY_ID=xxxx
#ENV AWS_SECRET_ACCESS_KEY=secret
#ENV AWS_ENDPOINT=s3-like-service:9000
ENV AWS_S3_FORCE_PATH_STYLE=true
ENV WALG_COMPRESSION_METHOD brotli

# See environment variables documentation https://github.com/wal-g/wal-g/blob/master/PostgreSQL.md#configuration
ENV PGHOST=/var/run/postgresql
ENV PGUSER=postgres
#ENV PGPASSWORD=secret
#ENV WALG_PGP_KEY_PATH=/keys/wal-g.pub

## Prometheus exporter
COPY --from=postgres_exporter /postgres_exporter /usr/local/bin/

## Overmind
RUN curl -L "https://github.com/DarthSim/overmind/releases/download/v${OVERMIND_VERSION}/overmind-v${OVERMIND_VERSION}-linux-amd64.gz" -o overmind.gz && \
    gunzip overmind.gz && \
    chmod +x overmind && \
    mv overmind /usr/local/bin/

WORKDIR /app
# Procfile should be located in the working directory (or -f path/to/your/Procfile)
ADD Procfile /app/
CMD ["overmind", "start"]
