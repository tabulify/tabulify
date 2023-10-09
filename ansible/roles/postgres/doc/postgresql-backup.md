# Backup and restore


https://www.postgresql.org/docs/15/backup.html

https://www.postgresql.org/docs/15/app-pgrestore.html#APP-PGRESTORE-EXAMPLES

## Cron Job

A simple backup scheme using cron keeps the latest monthly, weekly, daily, and
hourly backups as compressed plain text SQL files. Run the following to
restore a backup:

```
gunzip -c /var/lib/postgresql/backups/<name>.daily.sql.gz | psql <name>
```
