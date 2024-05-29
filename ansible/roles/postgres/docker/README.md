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

A dump restore is performed via the [ctl command](docker/bin/dbctl)

```bash
# List the available dump and select a snapshot
dbctl dump-ls
# perform a restore (the database is deleted!)
dbctl dump-restore snapshotId
# or for the latest one
dbctl dump-restore snapshotId
```

### How to perform a dump backup

A dump backup is performed via the [ctl command](docker/bin/dbctl)

```bash
dbctl dump-backup
# prune the repo
dbctl dump-prune
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

## dbctl

### Bash script

### Database procedure

You can't restore from the database as it expects a manual confirmation.

```sql
CALL public.dbctl('dump-ls');
```

## process

The process that are running in the container and their owner.

```bash
UID        PID  PPID  C STIME TTY          TIME CMD
root         1     0  0 14:14 ?        00:00:00 /bin/bash /usr/local/bin/entrypoint.sh overmind start
root         7     1  0 14:14 ?        00:00:00 overmind start
root        14     7  0 14:14 ?        00:00:00 tmux -C -L overmind---yqiI2DgpEldIIe3MQsPrQ new -n postgres -s - -P -F %overmind-process #{pane_id} postgres #{pane_pi
root        20     1  0 14:14 ?        00:00:00 tmux -C -L overmind---yqiI2DgpEldIIe3MQsPrQ new -n postgres -s - -P -F %overmind-process #{pane_id} postgres #{pane_pi
root        21    20  0 14:14 pts/0    00:00:00 sh /tmp/overmind---yqiI2DgpEldIIe3MQsPrQ/postgres
root        23    20  0 14:14 pts/1    00:00:00 sh /tmp/overmind---yqiI2DgpEldIIe3MQsPrQ/postgres_exporter
root        25    21  0 14:14 pts/0    00:00:00 /bin/bash /usr/local/bin/postgres-entrypoint.sh postgres -c config_file=/etc/postgresql/postgresql.conf
root        27    23  0 14:14 pts/1    00:00:00 postgres_exporter --log.level=warn
postgres    48    25  0 14:14 pts/0    00:00:00 postgres -c config_file=/etc/postgresql/postgresql.conf
postgres    69    48  0 14:14 ?        00:00:00 postgres: logger
postgres    70    48  0 14:14 ?        00:00:00 postgres: checkpointer
postgres    71    48  0 14:14 ?        00:00:00 postgres: background writer
postgres    73    48  0 14:14 ?        00:00:00 postgres: walwriter
postgres    74    48  0 14:14 ?        00:00:00 postgres: autovacuum launcher
postgres    75    48  0 14:14 ?        00:00:00 postgres: archiver
postgres    76    48  0 14:14 ?        00:00:00 postgres: pg_cron launcher
postgres    77    48  0 14:14 ?        00:00:00 postgres: logical replication launcher
postgres    78    48  0 14:14 ?        00:00:00 postgres: eraldy eraldy 172.17.0.1(47582) idle
root        95     0  0 14:16 pts/2    00:00:00 bash
root       323    95  0 14:20 pts/2    00:00:00 ps -ef
```

## Database: Postgres

After initialization, a database cluster will contain a database named postgres,
which is meant as a default database for use by utilities, users and third party applications.
The database server itself does not require the postgres database to exist,
but many external utility programs assume it exists.
https://www.postgresql.org/docs/9.1/creating-cluster.html

## Schema restoration

We don't use SQL restoration for schema
because with cascade, drops all objects that depend on the schema.
And this object may be external.

There is no guarantee that the results of a specific-schema dump
can be successfully restored into a clean database.

Why? Because the dump (pg_dump) will not dump any other database dependent objects
than the selected schema(s).

## Dump Format

* dir: one data file by tables. It will upload only the table changed
* sql: a sql file
* archive: one custom archive format



