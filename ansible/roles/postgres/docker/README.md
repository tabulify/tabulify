# Postgres

Iterative to debug
```
docker run --env-file secret.env --rm -it postgres_final
```

```
docker run --env-file secret.env --name postgres -d -p 5434:5432 postgres-final
```

* pgdata - /var/lib/postgresql/data

## How to perform a full backup

The backup location is set via:

```
WALG_S3_PREFIX=s3://postgres-dev/nico
```

In the container:

```
wal-g backup-push $PGDATA
```

## Init

* Database initialization only happens on container startup
* Run if you start the container with a data directory that is empty.

The (*.sql, *.sql.gz, or *.sh) init scripts

* should be located in the directory `/docker-entrypoint-initdb.d`
* are executed in sorted name order as defined by the current locale (default to en_US.utf8)

