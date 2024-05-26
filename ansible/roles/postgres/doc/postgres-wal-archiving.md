# Write Ahead Log Archiving (Point in Time recovery)

## About

called also:

* continuous archiving
* online backup

A restore requires:

* a full backup (file system or `pg_backup`)
* and one or more WAL segments in order to work correctly.

Note on [Wal archiving](https://www.postgresql.org/docs/current/continuous-archiving.html)

## Advantage

* No need of a file system snapshot (just tar or a similar archiving tool.)
* continuous backup can be achieved simply by continuing to archive the WAL files. This is particularly valuable for
  large databases or egress cost-effective, where it might not be convenient to take a full backup frequently.
* point-in-time recovery
* warm standby system by continuously feeding the WAL files to another machine

## WAL

WAL is a mechanism to ensure that no committed changes are lost.

Transactions are written sequentially to the WAL
and a transaction is considered to be committed when those writes are flushed to disk.

Afterwards, a background process writes the changes into the main database cluster files (also known as the heap).
In the event of a crash, the WAL is replayed to make the database consistent.

WAL is conceptually infinite but in practice is broken up into individual 16MB files called segments.

### WAL Name

The segment files are given numeric names that reflect their position in the abstract WAL sequence.

WAL segments follow the naming convention `0000000100000A1E000000FE` where:

* the first 8 hexadecimal digits represent the timeline
* the next 16 digits are the logical sequence number (LSN).

## Base Backup

[Base backup](https://www.postgresql.org/docs/current/app-pgbasebackup.html): The first backup of a database is always
a `Full Backup`.

Base backups are copies from your PostgreSQL as is.
These are needed to apply `PITR` because whenever you recover your database you can choose a base backup to be a
starting
point of the recovery.

## Data Lost in non-write-heavy

If your app is not write-heavy your WAL files might take time to ship the first finished page since
Postgres will wait until it fills an entire page (by default `16MB`) before shipping it.

A non-write heavy DB should take long to ship WAL files,
that could mean you could lose that data in a disaster recovery scenario.

## How to: Recovering Using a Continuous Archive Backup

`Recovery` covers using the server as a standby or for executing a targeted recovery.

Summary
of [Recovering Using a Continuous Archive Backup](https://www.postgresql.org/docs/current/continuous-archiving.html#BACKUP-PITR-RECOVERY)

1. Stop the server, if it's running.
2. Copy the whole cluster data directory `$PGDATA` and any tablespaces to a temporary location.
   At least save the contents of the cluster's `$PGDATA/pg_wal` subdirectory, as it might contain WAL files which were
   not archived before the system went down.
3. Remove all existing files and subdirectories under the cluster data directory and under the root directories of any
   tablespaces you are using.
4. Restore the database files from your file system backup. Be sure that they are restored with the right ownership (the
   database system user, not root!) and with the right permissions.
5. Remove any files present in `$PGDATA/pg_wal/` these came from the file system backup and are therefore probably
   obsolete rather than current.
6. If you have unarchived WAL segment files that you saved in step 2, copy (not move) them into `pg_wal/`.
7. Set recovery
   configuration [settings in postgresql.conf](https://www.postgresql.org/docs/current/runtime-config-wal.html#RUNTIME-CONFIG-WAL-ARCHIVE-RECOVERY)
   . Create a file `recovery.signal` in the cluster data directory `$PGDATA`
   . temporarily modify `pg_hba.conf` to prevent ordinary users from connecting until you are sure the recovery was
   successful.
8. Start the server. Upon completion of the recovery process, the server will
   . remove `recovery.signal`
   . commence normal database operations.

### Target (Recovery time)

By default, recovery will recover to the end of the WAL log, but you can specify another.

https://www.postgresql.org/docs/current/runtime-config-wal.html#RUNTIME-CONFIG-WAL-RECOVERY-TARGET

## Archive command

Example: copy archivable WAL segments to a directory

```ini
# Unix
archive_command = 'test ! -f /mnt/server/archivedir/%f && cp %p /mnt/server/archivedir/%f'
# Windows
archive_command = 'copy "%p" "C:\\server\\archivedir\\%f"'
```

where:

* `%p` is the path name of the file to archive (Ex: `pg_wal/00000001000000A900000065`)
* `%f` is the file name of the file to archive (Ex: `00000001000000A900000065`)
  The path name is relative to the current working directory, i.e., the cluster's data directory.

## Restore command

The [restore_command](https://www.postgresql.org/docs/current/runtime-config-wal.html#GUC-RESTORE-COMMAND)
tells Postgres how to retrieve archived WAL file segments.

```ini
restore_command = 'cp /mnt/server/archivedir/%f %p'
```

Note:

* Not all of the requested files will be WAL segment files: you should also expect requests for files with a suffix of
  `.history`.
* The base name of the `%p` path will be different from `%f`; do not expect them to be interchangeable.
* WAL segments that cannot be found in the archive will be sought in `pg_wal/` to allow use of recent un-archived
  segments.
* A normal recovery will end with a `file not found` message, the exact text of the error message depending upon your
  choice of restore_command.
* You may also see an error message at the start of recovery for a file named something like `00000001.history`. This is
  also normal

## Conf

### archive_mode

When archive_mode is enabled (on or always),
completed WAL segments are sent to archive storage
by setting `archive_command` or `archive_library`.

### Archive timeout

If the value is specified without units, it is taken as seconds.
The `archive_command` is only invoked for completed WAL segments.

With low traffic, the command would never be executed.
Archived files that are closed early due to a forced switch
are still the same length as completely full files.
Therefore, it is unwise to use a very short archive_timeout — it will bloat your archive storage.
Use Streaming replication, instead of archiving, if you want data to be copied off the primary server more quickly than
that.
https://www.postgresql.org/docs/current/runtime-config-wal.html#GUC-ARCHIVE-TIMEOUT

### logging_collector

When using an `archive_command` script, it's desirable to enable `logging_collector`.

Any messages written to stderr from the script will then appear in the database server log, allowing complex
configurations to be diagnosed easily if they fail.

## Tool

* [wal-g](postgres-wal-g.md)
* https://pgbackrest.org/user-guide.html
* https://github.com/EnterpriseDB/barman (Backup and Recovery Manager)

### Fly use Barman

https://fly.io/docs/flyctl/postgres-barman/
https://community.fly.io/t/point-in-time-recovery-for-postgres-flex-using-barman/13185

`fly pg barman create` will create a machine in your Postgres cluster with barman ready to use.

Why Barman? Barman has great support for streaming replication
to store WAL files and also works well with `repmgr`

