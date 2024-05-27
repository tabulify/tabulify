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
