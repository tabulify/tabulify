# Drill Installation


## About

Install Drill in a single node (ie embedded mode)

## Run

```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags drill
```

## Start

If you use Drill in embedded mode, you do not use the drillbit command.

```bash
export DRILL_HEAP=500M
bin/drill-embedded
```


In embedded mode, invoking SQLLine automatically starts a Drillbit locally.
This example starts SQLLine on a node in an embedded, single-node cluster:
```bash
sqlline -u jdbc:drill:zk=local
# same as drill-embedded
```

## Stop

From [Stopping Drill](https://svn-eu.apache.org/repos/asf/drill/site/trunk/content/drill/docs/starting-stopping-drill/index.html)
```bash
kill -9 `ps auwx | grep drill | grep sqlline | awk '{print $2}'`
```

## Test

### Web Ui
  * Access, add a record into the host file to the `beau.bytle.net` ip for `https://drill.bytle.net`
  * Query
```sql
SELECT * FROM dfs.`/opt/apache-drill-1.20.0/sample-data/region.parquet`
```

### Jdbc

Because this is an embedded installation (one node), this is a [direct drill bit connection](https://drill.apache.org/docs/using-the-jdbc-driver/#using-the-jdbc-url-format-for-a-direct-drillbit-connection)
```bash
bin/sqlline -u "jdbc:drill:drillbit=localhost:31010" -n drill -p xxxx
```

With Dbeaver:
  * With ssh tunnel
    * SSH: User and password
    * Advanced Setting: Local Port 31010, Remote Port 31010
  * via Nginx tcp proxy: port is 31009, redirect to 310010. [Doc](https://drill.apache.org/docs/configuring-ssl-tls-for-encryption/#jdbc-connection-parameters)

```bash
./sqlline -u "jdbc:drill:schema=dfs.work;drillbit=drill.bytle.net:31009;enableTLS=true;"
```

Drill does not support client certificate.

## Log

```bash
journalctl -u drill.service --since "1 hour ago"
```

## Security

```sql
SELECT name, val FROM sys.`options` where name like 'security.admin%' ORDER BY name;
```
```txt
|name                      |val                        |
|--------------------------|---------------------------|
|security.admin.user_groups|%drill_process_user_groups%|
|security.admin.users      |%drill_process_user%       |
```

## Shell

The Drill shell is a command line sql statement.

Drill follows the SQL:2011 standard with [extensions](https://drill.apache.org/docs/sql-extensions/)
for nested data formats and other capabilities.

## Ref
  * [Drill in 10 minutes](https://drill.apache.org/docs/drill-in-10-minutes/)

