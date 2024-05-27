# Postgres

* Iterative to debug the created docker file
```
docker run --env-file secret.env --rm -it postgres_final
```

* To run with `pgdata` (/var/lib/postgresql/data) on
  the [disk](https://github.com/docker-library/docs/blob/master/postgres/README.md#where-to-store-data)

```bat
REM on windows
docker run --env-file secret.env --name postgres -d -p 5434:5432 -v C:\temp\data:/var/lib/postgresql/data postgres-final
```

## Dump

### How to perform a dump restore

A dump restore is performed via the [ctl command](docker/bin/ctl)

```bash
# List the available dump and select a snapshot
ctl dump-ls
# perform a restore (the database is deleted!)
ctl dump-restore snapshotId
# or for the latest one
ctl dump-restore snapshotId
```

### How to perform a dump backup

A dump backup is performed via the [ctl command](docker/bin/ctl)

```bash
ctl dump-backup
# prune the repo
ctl dump-prune
```

## How to perform a full backup

The backup location is set via:

```
WALG_S3_PREFIX=s3://bucket-name/path
WALG_S3_PREFIX=s3://postgres-dev/dev-name
```

In the container:
```
wal-g backup-push $PGDATA
```



## Diagnostic

Log is on at `$PGDATA/log`

## Env

When creating the image. See [secxx](secxx.env)

```ini
# See environment variables documentation \
# https://github.com/wal-g/wal-g/blob/master/docs/STORAGES.md
WALE_S3_PREFIX = s3://bucket-name/path/to/folder
AWS_ACCESS_KEY_ID = xxxx
AWS_SECRET_ACCESS_KEY = secret
AWS_ENDPOINT = s3-like-service:9000
WALG_COMPRESSION_METHOD = brotli
```
