# Backup and restore

https://www.postgresql.org/docs/15/backup.html

## Base Backup

[Base backup](https://www.postgresql.org/docs/current/app-pgbasebackup.html): The first backup of a database is always
a `Full Backup`.

Base backups are copies from your PostgreSQL as is.
These are needed to apply PITR because whenever you recover your database you can choose a base backup to be a starting
point
of the recovery and then `barman` will ship the missing WAL files from that point forward.

* Delta/Differential Backup: wal/files that have changed since the last full backup.

## Type

### Dump and restore

`pg_dump` and `pg_restore`
https://www.postgresql.org/docs/15/app-pgrestore.html#APP-PGRESTORE-EXAMPLES

### Wal

[PITR/Wal](postgres-wal-archiving.md)
https://www.postgresql.org/docs/current/continuous-archiving.html

## Tool

* [wal-g](postgres-wal-g.md)
* https://pgbackrest.org/user-guide.html
* https://github.com/EnterpriseDB/barman (Backup and Recovery Manager) -

### Fly

https://fly.io/docs/flyctl/postgres-barman/
https://community.fly.io/t/point-in-time-recovery-for-postgres-flex-using-barman/13185
`fly pg barman create` will create a machine in your Postgres cluster with barman ready to use.

Why Barman? Barman has great support for streaming replication
to store WAL files and also works well with `repmgr`

### Cron Job

A simple backup scheme using cron keeps the latest monthly, weekly, daily, and
hourly backups as compressed plain text SQL files. Run the following to
restore a backup:

```
gunzip -c /var/lib/postgresql/backups/<name>.daily.sql.gz | psql <name>
```

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
