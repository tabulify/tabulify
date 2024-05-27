# Write Ahead Log Archiving (Point in Time recovery)

## About

called also:

* continuous archiving
* online backup

A restore requires:

* a full backup (file system or `pg_backup`)
* and one or more WAL segments in order to work correctly.

Note on [Wal archiving](https://www.postgresql.org/docs/current/continuous-archiving.html)

In an abstract sense, a running PostgreSQL system produces an indefinitely long sequence of WAL records.

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

WAL Segment files are given ever-increasing numbers.

They follow the naming convention `0000000100000A1E000000FE` where:

* the first 8 hexadecimal digits represent the [timeline](#timeline)
* the next 16 digits are the [logical sequence number (LSN)](#log-sequence-number-lsn)

### Log Sequence Number (LSN)

Log Sequence Number (LSN) is a byte offset into the WAL, increasing monotonically with each new record.

### wal segment size

Wal file are called segment.

Segment files have a zie of 16 MB
The size can be changed by altering the `--wal-segsize initdb` option.

### wal structures

Each segment is divided into pages, normally 8 kB each.

The segment size can be changed via the `--with-wal-blocksize` configure option.

## Base Backup

[Base backup](https://www.postgresql.org/docs/current/app-pgbasebackup.html): The first backup of a database is always
a `Full Backup`.

Base backups are copies from your PostgreSQL as is.
These are needed to apply `PITR` because whenever you recover your database you can choose a base backup to be a
starting
point of the recovery.

### How to create bas backup (pg_basebackup

To create a base backup either as regular files or as a tar archive.

* `pg_basebackup`
* [low leve api](https://www.postgresql.org/docs/current/continuous-archiving.html#BACKUP-LOWLEVEL-BASE-BACKUP)

### Backup history file

To make use of the base backup, you need to keep all the WAL segment files generated during
and after the file system backup.

To aid you in doing this, the base backup process creates a backup history file
that is immediately stored into the WAL archive area.

This file is named after the first WAL segment file that you need for the file system backup.

For example, if the starting WAL file is `0000000100001234000055CD` the backup history file will be named something
like `0000000100001234000055CD.007C9330.backup`.
(The second part of the file name stands for an exact position within the WAL file, and can ordinarily be ignored.)

The backup history file is just a small text file that contains

* the label string you gave to `pg_basebackup`
* the starting and ending times
* WAL segments of the backup.

```txt
START WAL LOCATION: 0/6000028 (file 000000010000000000000006)
STOP WAL LOCATION: 0/6000138 (file 000000010000000000000006)
CHECKPOINT LOCATION: 0/6000060
BACKUP METHOD: streamed
BACKUP FROM: primary
START TIME: 2024-05-26 15:08:55 UTC
LABEL: 2024-05-26 15:08:54.771982 +0000 UTC m=+0.079869405
START TIMELINE: 1
STOP TIME: 2024-05-26 15:08:59 UTC
STOP TIMELINE: 1
```

Once you have safely archived the file system backup and the WAL segment files
used during the backup (as specified in the backup history file),
all archived WAL segments with names numerically less are no longer needed to recover the file system backup
and can be deleted.
However, you should consider keeping several backup sets
to be absolutely certain that you can recover your data.

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

It is not necessary to preserve the original relative path (%p) but it is necessary to preserve the file name (%f).

### How to stop archiving temporally

`archive_command` and `archive_library` can be changed with a configuration file reload.
If you are archiving via shell and wish to temporarily stop archiving, one way to do it is to set archive_command to the
empty string ('').

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

### Wal segment switch (Archive timeout, pg_switch_wal function)

`archive_timeout` option is used to force a WAL segment switch
at interval as the `archive_command` is only invoked for completed WAL segments.
(16Mb default)

If the value is specified without units, it is taken as seconds.

You can force a segment switch manually with
[pg_switch_wal](https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-ADMIN-BACKUP-TABLE)

With low traffic, the command would never be executed.

* Archived files that are archived/closed early due to a forced switch are still the same length as completely full
  files.
* It is unwise to use a very short `archive_timeout` — it will bloat your archive storage.
* Use Streaming replication, instead of archiving, if you want data to be copied off the primary server more quickly
  than
that.

https://www.postgresql.org/docs/current/runtime-config-wal.html#GUC-ARCHIVE-TIMEOUT


### logging_collector

When using an `archive_command` script, it's desirable to enable `logging_collector` (log are
stored in the `${PGDATA}/log` directory)

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

## The Data Directory (and Back Up)

https://www.postgresql.org/docs/current/continuous-archiving.html#BACKUP-LOWLEVEL-BASE-BACKUP-DATA
You should omit from the backup the files within the cluster's pg_wal/ subdirectory. This slight adjustment is
worthwhile because it reduces the risk of mistakes when restoring.
You might also want to exclude postmaster.pid and postmaster.opts, which record information about the running
postmaster.
It is often a good idea to also omit from the backup the files within the cluster's pg_replslot/ directory, so that
replication slots that exist on the primary do not become part of the backup.
.... there is more to be omitted

### pg_wal

The `pg_wal/` directory will continue to fill with WAL segment files until the situation is resolved (archive command
return successfully). (If the file system containing pg_wal/ fills up, PostgreSQL will do a PANIC shutdown.
No committed transactions will be lost, but the database will remain offline until you free some space.)

### conf files

WAL archiving will not restore changes made to [configuration files](postgres-conf.md)
since those are edited manually rather than through SQL operations.

## Monitoring

* The `pg_stat_archiver` view always have a single row,
  containing data about the archiver process of the cluster. (Failure may be not reported)
  https://www.postgresql.org/docs/current/monitoring-stats.html#PG-STAT-ARCHIVER-VIEW

* `pg_wal/` directory should not contain large numbers of not-yet-archived segment files

## Timeline

Whenever an archive recovery completes, a new timeline is created to identify
the series of WAL records generated after a recovery.

The timeline ID number is part of WAL segment file names so a new timeline does not overwrite
the WAL data generated by previous timelines.

For example, in the WAL file name `0000000100001234000055CD`, the leading `00000001` is the timeline ID in hexadecimal.

Every time a new timeline is created, Postgres creates a `timeline history` file
that shows which timeline it branched
off from and when.

```
1	0/9000000	no recovery target specified

2	0/A000000	no recovery target specified
```

This history files are just small text files, so it's cheap and appropriate to keep them around
indefinitely (unlike the segment files which are large).

The default behavior of recovery is to recover to the latest timeline found in the archive.
(You can specify current or the target timeline ID in recovery_target_timeline.)

## Replication slots

Replication slots provide an automated way to ensure
that the primary does not remove WAL segments until they have been received by all standbys

## Backup function

https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-ADMIN-BACKUP

* Return the current write-ahead log write location

```sql
select *
from pg_current_wal_lsn();
```

```
0/B000000
```

* Converts a write-ahead log location to the name of the WAL file holding that location

```sql
select *
from pg_walfile_name(pg_lsn)
-- current wal file writed (example: 00000003000000000000000A)
select *
from pg_walfile_name(pg_current_wal_lsn())
-- current write-ahead log flush location
select *
from pg_walfile_name(pg_current_wal_flush_lsn())
-- current write-ahead log insert location
select *
from pg_walfile_name(pg_current_wal_insert_lsn())
```

* Extracts the sequence number and timeline ID from a WAL file name.

```sql
select * from pg_split_walfile_name('00000003000000000000000A');
```

+--------------+-----------+
|segment_number|timeline_id|
+--------------+-----------+
|10 |3 |
+--------------+-----------+

* The last write-ahead log location that has been replayed during recovery.

```sql
select * from pg_last_wal_replay_lsn ()
--exaample: 000000030000000000000009
-- wal file name
select * from pg_walfile_name(pg_last_wal_replay_lsn ());
```

* The time stamp of the last transaction replayed during recovery

```sql
select * from pg_last_xact_replay_timestamp ();
```

+---------------------------------+
|pg_last_xact_replay_timestamp |
+---------------------------------+
|2024-05-26 17:11:06.105773 +00:00|
+---------------------------------+

* the name, size, and last modification time (mtime) of each ordinary file in the server's write-ahead log (WAL)
  directory.

```sql
select * from pg_ls_waldir ();
```

+------------------------+--------+---------------------------------+
|name |size |modification |
+------------------------+--------+---------------------------------+
|00000002.history |41 |2024-05-26 17:16:02.000000 +00:00|
|00000003.history |83 |2024-05-26 17:16:32.000000 +00:00|
|00000003000000000000000A|16777216|2024-05-26 19:56:16.000000 +00:00|
|00000003000000000000000B|16777216|2024-05-26 17:16:03.000000 +00:00|
|00000003000000000000000C|16777216|2024-05-26 17:16:13.000000 +00:00|
|00000003000000000000000D|16777216|2024-05-26 17:16:19.000000 +00:00|
|00000003000000000000000E|16777216|2024-05-26 17:16:28.000000 +00:00|
+------------------------+--------+---------------------------------+
