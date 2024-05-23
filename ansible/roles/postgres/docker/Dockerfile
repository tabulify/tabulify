# https://hub.docker.com/_/postgres
ARG PG_VERSION=16.3
ARG PG_DOCKER_TAG=16.3-alpine
ARG PG_MAJOR_VERSION=16


# Postgres Exporter
# https://github.com/prometheus-community/postgres_exporter
FROM wrouesnel/postgres_exporter:latest AS postgres_exporter

FROM postgres:${PG_DOCKER_TAG} as final
ENV PGDATA=/data/postgresql
ENV PGPASSFILE=/data/.pgpass
ARG VERSION

RUN apk update \
    && apk add gomplate

COPY --from=wal-g /wal-g /

# PostGIS
RUN apt-get update && apt-get install --no-install-recommends -y \
    postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR \
    postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR-scripts

LABEL app_role=postgres_cluster
LABEL version=${VERSION}
LABEL pg-version=${PG_VERSION}
LABEL pg-manager=repmgr

ENV ARCHIVE_MODE=off
# When archive_mode is enabled (on or always),
# completed WAL segments are sent to archive storage by setting archive_command or archive_library.
# Off by default to avoid crushing PostgreSQL production data by mistake

ENV ARCHIVE_TIMEOUT=0
# Archive timeout
# The archive_command is only invoked for completed WAL segments.
# With low traffic, the command would never be executed.
# Archived files that are closed early due to a forced switch
# are still the same length as completely full files.
# Therefore, it is unwise to use a very short archive_timeout — it will bloat your archive storage.
# Use Streaming replication, instead of archiving, if you want data to be copied off the primary server more quickly than that.
# https://www.postgresql.org/docs/current/runtime-config-wal.html#GUC-ARCHIVE-TIMEOUT

RUN mkdir -p /etc/postgresql/ \
    && cp /usr/local/share/postgresql/postgresql.conf.sample /etc/postgresql/postgresql.conf.tmpl \
    && sed -ri "s/^#archive_mode = off/archive_mode = {{.Env.ARCHIVE_MODE}}/" /etc/postgresql/postgresql.conf.tmpl \
    && sed -ri "s/^#archive_timeout = 0/archive_timeout = {{.Env.ARCHIVE_TIMEOUT}}/" /etc/postgresql/postgresql.conf.tmpl \
    && sed -ri "s/^#archive_command = ''/archive_command = '\/wal-g wal-push %p'/" /etc/postgresql/postgresql.conf.tmpl \
    && sed -ri "s/^#restore_command = ''/restore_command = '\/wal-g wal-fetch %f %p'/" /etc/postgresql/postgresql.conf.tmpl

# enable the archive mode with archive command
RUN echo "archive_mode = yes" >> /usr/local/var/pg15/postgresql.conf
RUN echo "archive_command = 'envdir /etc/wal-g.d/env /usr/local/bin/wal-g wal-push %p'" >> /usr/local/var/pg15/postgresql.conf
RUN echo "archive_timeout = 60" >> /usr/local/var/pg15/postgresql.conf \

ADD docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod u+x /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh"]

# See environment variables documentation https://github.com/wal-g/wal-g#configuration
#ENV WALE_S3_PREFIX=s3://bucket/path/to/folder
#ENV AWS_ACCESS_KEY_ID=xxxx
#ENV AWS_SECRET_ACCESS_KEY=secret
#ENV AWS_REGION=us-west-2
#ENV AWS_ENDPOINT=http://s3-like-service:9000
ENV AWS_S3_FORCE_PATH_STYLE=true
ENV WALG_COMPRESSION_METHOD brotli

# See environment variables documentation https://github.com/wal-g/wal-g/blob/master/PostgreSQL.md#configuration
ENV PGHOST=/var/run/postgresql
ENV PGUSER=postgres
#ENV PGPASSWORD=secret
#ENV WALG_PGP_KEY_PATH=/keys/wal-g.pub

COPY --from=postgres_exporter /postgres_exporter /usr/local/bin/

CMD ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf"]
