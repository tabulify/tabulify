# Backup and restore

https://www.postgresql.org/docs/15/backup.html

## Dump: Backup in PSQL format

The whole database in a PSQL file.

Example:

* Backup

```bash
pg_dump dbname | gzip  > $PGDATA/dump/dumpfile.sql.gz
```

* Restore

```bash
cat $PGDATA/dump/dumpfile.sql.gz | gunzip | psql --set ON_ERROR_STOP=on --single-transaction dbname
# run analyze (to update the stats)
```

where:

* `ON_ERROR_STOP` - stop on error
* `single-transaction` will commit only at the end (to avoid a partially restored dump)

You can also [split](https://www.postgresql.org/docs/current/backup-dump.html#BACKUP-DUMP-LARGE)

## Dump and restore: Backup in custom dump format

Same as gzip with the advantage that tables can be restored selectively `pg_dump` and `pg_restore`

[example](https://www.postgresql.org/docs/15/app-pgrestore.html#APP-PGRESTORE-EXAMPLES)
[ref](https://www.postgresql.org/docs/current/backup-dump.html#BACKUP-DUMP-LARGE)

### In a single process

* Dump

```bash
pg_dump -Fc dbname > filename.dump
```

* Restore in the same database

```bash
# dropdb dbname
pg_restore -d dbname filename.dump
```

* Restore in another database

```bash
# the database is created from template0 not template1, to ensure it is initially empty.
createdb -T template0 newdb
# -C is not used to connect directly to the database to be restored into.
pg_restore -d newdb db.dump
```

### In parallel (big database)

```bash
pg_dump -j parralelNum -F d -f out.dir dbname
```

```bash
pg_restore -j
```

### File system

Don't as the database needs to be shutdown.

### Continuous Archiving and Point-in-Time Recovery (PITR/Wal)

[PITR/Wal](postgres-wal-archiving.md)


## Backup Script from Postgres

With `pg_cron` and `python`

Note: Only superusers can create functions in untrusted languages such as `plpython3u`.
https://www.postgresql.org/docs/current/plpython.html

```
CREATE EXTENSION plpythonu;
```

```sql
CREATE
OR REPLACE FUNCTION backup(command text)
RETURNS text AS $$
import subprocess
result = subprocess.run("backup.sh", shell=True, capture_output=True, text=True)
return result.stdout + result.stderr
$$ LANGUAGE plpythonu;
```

```sql
SELECT exec_shell('ls -la');
```
