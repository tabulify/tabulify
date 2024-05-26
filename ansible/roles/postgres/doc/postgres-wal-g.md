# Wal-g

## Backup Scenario

[](https://github.com/stephane-klein/playground-postgresql-walg/blob/master/README.md)

### Create a container


```bash
# windows
docker run --env-file secret.env --name postgres -d -p 5434:5432 -v C:\temp\data:/var/lib/postgresql/data postgres-final
# git bash
docker run --env-file secret.env --name postgres2 -d -p 5434:5432 -v C:/temp/data2:/var/lib/postgresql/data postgres-final
```


### Connect

* Go into the container

```bash
docker exec -it postgres bash
# git bash
winpty docker exec -it postgres bash
```

* SQL Client: localhost: 5434

### Make base backup

```bash
wal-g backup-push -f $PGDATA
```

### Monitoring

Check Stats archiver Informations

```bash
echo "select * from pg_stat_archiver;" | psql -x -U $POSTGRES_USER $POSTGRES_DB -a -q -f -
```

[pg_stat_archiver](https://www.postgresql.org/docs/current/monitoring-stats.html#MONITORING-PG-STAT-ARCHIVER-VIEW)
```
-[ RECORD 1 ]------+-----------------------------------------
archived_count     | 4    # Number of WAL files that have been successfully archived
last_archived_wal  | 000000010000000000000003.00000028.backup # Name of the WAL file most recently successfully archived
last_archived_time | 2020-04-01 22:12:52.273572+00
failed_count       | 0    # Number of failed attempts for archiving WAL files
last_failed_wal    |      # Name of the WAL file of the most recent failed archival operation
last_failed_time   |      # Time of the most recent failed archival operation
stats_reset        | 2020-04-01 22:12:19.392116+00
```

* switch a wal manually
```sql
SELECT pg_switch_wal()
```

```
SHOW archive_mode;
SHOW archive_command;
```

### Generate Data

```bash
pgbench -U $PGUSER -i -s 2 -n
```

### wal-g backup-list

```bash
INFO : 2024/05/25 09:03:52.466361 List backups from storages: [default]
backup_name                   modified             wal_file_name            storage_name
base_000000010000000000000003 2024-05-25T08:49:00Z 000000010000000000000003 default
base_000000010000000000000006 2024-05-25T08:51:59Z 000000010000000000000006 default
base_000000010000000000000008 2024-05-25T08:52:15Z 000000010000000000000008 default
base_000000010000000000000016 2024-05-25T09:02:40Z 000000010000000000000016 default
```

### wal-g wal-show

wal-g wal-show

```
+-----+------------+-----------------+--------------------------+--------------------------+---------------+----------------+--------+---------------+
| TLI | PARENT TLI | SWITCHPOINT LSN | START SEGMENT            | END SEGMENT              | SEGMENT RANGE | SEGMENTS COUNT | STATUS | BACKUPS COUNT |
+-----+------------+-----------------+--------------------------+--------------------------+---------------+----------------+--------+---------------+
|   1 |          0 |             0/0 | 000000010000000000000001 | 000000010000000000000014 |            20 |             20 | OK     |             3 |
+-----+------------+-----------------+--------------------------+--------------------------+---------------+----------------+--------+---------------+
```

after a full backup

```
+-----+------------+-----------------+--------------------------+--------------------------+---------------+----------------+--------+---------------+
| TLI | PARENT TLI | SWITCHPOINT LSN | START SEGMENT            | END SEGMENT              | SEGMENT RANGE | SEGMENTS COUNT | STATUS | BACKUPS COUNT |
+-----+------------+-----------------+--------------------------+--------------------------+---------------+----------------+--------+---------------+
|   1 |          0 |             0/0 | 000000010000000000000001 | 000000010000000000000016 |            22 |             22 | OK     |             4 |
+-----+------------+-----------------+--------------------------+--------------------------+---------------+----------------+--------+---------------+
```

### Restore

```bash
docker run --rm postgres2 sh -c 'wal-g backup-fetch $PGDATA LATEST; touch $PGDATA/recovery.signal'
```

## Check / Verify

https://github.com/wal-g/wal-g/blob/a2c015d8d22289877f548c3ee2a9cbed5695ce33/docs/PostgreSQL.md#wal-verify

```
wal-g backup-list
wal-g wal-show
wal-g wal-verify integrity timeline
```

## wal deletion

Postgres recycles wal files, so you should see the number of files
remaining pretty much the same, but the file names should change
during the time.

## wal-g wal-push

```
export WALG_PREVENT_WAL_OVERWRITE=1; wal-g wal-push $PGDATA/pg_wal/00000003000000000000000B
```

## Delta

with `WALG_DELTA_MAX_STEPS` to a number greater than 0

deltas and base backup
[](https://github.com/wal-g/wal-g/issues/187#issuecomment-469770129)

Deltas is a whole new story.
Delta-backup is a backup which can be applied to base backup.
But much faster than WAL, because it is parallel and squashes multiple page writes into one.

## Example / command List

```bash
# To list the backups
wal-g backup-list

# Delete the older than 5 days backup
wal-g delete retain Full 5 --confirm
```

## Benchmark

```bash
# base backup
wal-g backup-push /var/lib/postgresql/10/main/
# https://www.postgresql.org/docs/current/pgbench.html
pgbench -U $PGUSER -i -s $SCALE -n
# 1600 MB
pgbench -i -s 1000 userdb
```

https://github.com/wal-g/wal-g/blob/master/benchmarks/reverse-delta-unpack/reverse-delta-unpack-26-03-2020.md
https://github.com/wal-g/wal-g/blob/master/benchmarks

## Exporter

The [exporter](https://github.com/wal-g/wal-g/issues/323#issuecomment-595663310) have the following metrics :

* total wal segments: total wal segments on remote storage
* continuous wal segments: total wal segments without gap starting from last uploaded
* valid base backup: total base backup starting from last uploaded without wal segments gap
* missing wal segments: Missing wal segment on remote storage between base backups
* missing wal segments at end: Missing wal segment near the master position, should not go higher than 1. Replication
  lag for remote storage
