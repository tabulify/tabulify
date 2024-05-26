# Postgres Conf

* postgresql.conf
* pg_hba.conf
* and pg_ident.conf

## reload

If you edit a conf file on a running system, you have to

* SIGHUP the server for the changes to take effect,
* run "pg_ctl reload",
* or execute "SELECT pg_reload_conf()".

## Client authentication by a pg_hba.conf

Allow the user "foo" from host 192.168.1.100 to connect to the primary

```ini
# as a replication standby if the user's password is correctly supplied.
#
# TYPE  DATABASE        USER            ADDRESS                 METHOD
host    replication     foo             192.168.1.100/32        md5
```
