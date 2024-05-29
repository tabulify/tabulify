# Restoration

## In a new db

```bash
dbctl db-archive-restore latest newdb
```

The default is to continue and to display a count of errors at the end of the restoration.
Restoration in a `newdb` will give error because of the extensions but will succeed.

```bash
pg_restore: error: could not execute query: ERROR:  can only create extension in database postgres
DETAIL:  Jobs must be scheduled from the database configured in cron.database_name, since the pg_cron background worker reads job descriptions from this database.
HINT:  Add cron.database_name = 'newdb' in postgresql.conf to use the current database.
CONTEXT:  PL/pgSQL function inline_code_block line 4 at RAISE
Command was: CREATE EXTENSION IF NOT EXISTS pg_cron WITH SCHEMA pg_catalog;


pg_restore: error: could not execute query: ERROR:  extension "pg_cron" does not exist
Command was: COMMENT ON EXTENSION pg_cron IS 'Job scheduler for PostgreSQL';


pg_restore: error: could not execute query: ERROR:  schema "cron" does not exist
Command was: COPY cron.job (jobid, schedule, command, nodename, nodeport, database, username, active, jobname) FROM stdin;
pg_restore: error: could not execute query: ERROR:  schema "cron" does not exist
Command was: COPY cron.job_run_details (jobid, runid, job_pid, database, username, command, status, return_message, start_time, end_time) FROM stdin;
pg_restore: error: could not execute query: ERROR:  relation "cron.jobid_seq" does not exist
LINE 1: SELECT pg_catalog.setval('cron.jobid_seq', 1, false);
^
Command was: SELECT pg_catalog.setval('cron.jobid_seq', 1, false);


pg_restore: error: could not execute query: ERROR:  relation "cron.runid_seq" does not exist
LINE 1: SELECT pg_catalog.setval('cron.runid_seq', 1, false);
^
Command was: SELECT pg_catalog.setval('cron.runid_seq', 1, false);


pg_restore: warning: errors ignored on restore: 6
```
