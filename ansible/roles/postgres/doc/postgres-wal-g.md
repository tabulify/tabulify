# Wal-g

## Backup Scenario

[](https://github.com/stephane-klein/playground-postgresql-walg/blob/master/README.md)

* Make base backup

```bash
wal-g backup-push -f $PGDATA
```

* Check Stats archiver Informations

```bash
echo "select * from pg_stat_archiver;" | psql -x -U $POSTGRES_USER $POSTGRES_DB -a -q -f -
```

```
-[ RECORD 1 ]------+-----------------------------------------
archived_count     | 4
last_archived_wal  | 000000010000000000000003.00000028.backup
last_archived_time | 2020-04-01 22:12:52.273572+00
failed_count       | 0
last_failed_wal    |
last_failed_time   |
stats_reset        | 2020-04-01 22:12:19.392116+00
```

* Restore

```bash
docker run --rm postgres2 sh -c 'wal-g backup-fetch $PGDATA LATEST; touch $PGDATA/recovery.signal'
```

```bash
fly ssh console

```

## Check / Verify

https://github.com/wal-g/wal-g/blob/a2c015d8d22289877f548c3ee2a9cbed5695ce33/docs/PostgreSQL.md#wal-verify

```
wal-g backup-list
wal-g wal-show
wal-g wal-verify integrity timeline
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
