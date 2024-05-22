# Postgres


## Run

```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags postgres -v
```

## Topology

Cluster (Running Instance)> database

Every instance of a running PostgreSQL server manages one or more databases .
via:
* https://www.postgresql.org/docs/15/sql-createdatabase.html
* https://www.postgresql.org/docs/15/app-createdb.html

## Service

```bash
systemctl status postgresql-15
```
```
● postgresql-15.service - PostgreSQL 15 database server
Loaded: loaded (/usr/lib/systemd/system/postgresql-15.service; enabled; vendor preset: disabled)
Active: active (running) since Fri 2022-11-11 11:33:44 UTC; 2min 10s ago
Docs: https://www.postgresql.org/docs/15/static/
Process: 19220 ExecStartPre=/usr/pgsql-15/bin/postgresql-15-check-db-dir ${PGDATA} (code=exited, status=0/SUCCESS)
Main PID: 19226 (postmaster)
Tasks: 7
Memory: 32.9M
CGroup: /system.slice/postgresql-15.service
├─19226 /usr/pgsql-15/bin/postmaster -D /var/lib/pgsql/15/data/
├─19228 postgres: logger
├─19229 postgres: checkpointer
├─19230 postgres: background writer
├─19232 postgres: walwriter
├─19233 postgres: autovacuum launcher
└─19234 postgres: logical replication launcher
```

```bash
sudo systemctl start postgresql-15
sudo systemctl restart postgresql-15
sudo systemctl enable postgresql-15
```

## User Account
Postgres is the PostgreSQL User Account

```bash
sudo -i -u postgres
```

## Psql Client

```bash
sudo -i -u postgres
# then
psql
# then to connect to the postgres database
\c postgres
# then to see the schema
\dn
```


More
```bash
# then to get a list of command
\?
```

```bash
\q            : exit
\list or \l   : list all databases
\c <db name>  : connect to a certain database
\dt           : list all tables in the current database using your search_path
\dt *         : list all tables in the current database regardless your search_path
\du           : user account
```

## Admin Client

https://www.postgresql.org/docs/15/reference-client.html

## Base Directory

Directory: `/var/lib/pgsql/15/`

## Configuration

Parameters are in the file `postgresql.conf`
A default copy is installed when the database cluster directory is initialized.

## Sql Conf script

To add extension and modify any configuration, we use
the [db-conf.sql](templates/db-conf.sql) script

## Ref

https://www.postgresql.org/download/linux/redhat/


## Ansible Collections

Ansible as a collection of utility:
https://galaxy.ansible.com/community/postgresql
