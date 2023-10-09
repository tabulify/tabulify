# Postgres Client connection


## About
When a client application connects to the database server,
it specifies which PostgreSQL database user name it wants to connect


## User

Within the SQL environment, the active database user name determines access privileges to database objects
Therefore, it is essential to restrict which database users can connect.

```bash
sudo -i -u postgres
createuser --interactive --pwprompt
```

## Authentication

[Client authentication](https://www.postgresql.org/docs/15/auth-pg-hba-conf.html)
is controlled by the configuration file `{{ pg_data}}\pg_hba.conf`

The pg_hba.conf records are examined sequentially for each connection attempt,
the order of the records is significant.

See [pg_hba.conf](../templates/pg_hba.conf.ini)
