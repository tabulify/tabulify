# Cassandra Installation



## Run

```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags cassandra
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags cassandra-conf
```

## Command Line

```bash
/sbin/cassandra -h
# link to
/usr/sbin/cassandra
```


## Directory Layout

```bash
cat /etc/rc.d/init.d/cassandra
```
```bash
export CASSANDRA_HOME=/usr/share/cassandra
export CASSANDRA_CONF=/etc/cassandra/conf
export CASSANDRA_INCLUDE=$CASSANDRA_HOME/cassandra.in.sh
export CASSANDRA_OWNR=cassandra
NAME="cassandra"
log_file=/var/log/cassandra/cassandra.log
pid_file=/var/run/cassandra/cassandra.pid
lock_file=/var/lock/subsys/$NAME
CASSANDRA_PROG=/usr/sbin/cassandra
```
Directory containing logs(default: $CASSANDRA_HOME/logs)

## Port

```bash
nmap -Pn -p T:9042 localhost
```

## Sql
  * cqlsh
```bash
cqlsh localhost
```
```sql
SELECT cluster_name, listen_address FROM system.local;
create keyspace test with replication = {'class':'SimpleStrategy','replication_factor':1};
use test;
```
  * Idea
  * Drill
```sql
show schemas
SELECT * FROM cassandra.<keyspace_name>.<table_name> LIMIT 10;
```

For test query, see:
  * [Cassandra Adapter](https://calcite.apache.org/docs/cassandra_adapter.html)
  * [DrillTest](https://github.com/apache/drill/tree/master/contrib/storage-cassandra/src/test/java/org/apache/drill/exec/store/cassandra))
    https://confusedcoders.com/data-engineering/apache-drill/sql-on-cassandra-querying-cassandra-via-apache-drill

## Configuration

/etc/cassandra/default.conf/cassandra.yaml

https://cassandra.apache.org/doc/latest/cassandra/configuration/index.html

## Port

```yaml

  - 7199/tcp  # jmx
  - 9160/tcp  # Thrift client API
# Internode communication (not used if TLS enabled)
# For security reasons, you should not expose this port to the internet.
storage_port: 7000
# TLS Internode communication (used if TLS enabled)
# For security reasons, you should not expose this port to the internet.
ssl_storage_port: 7001
# port for the CQL native transport to listen for clients on
# For security reasons, you should not expose this port to the internet.  Firewall it if needed.
native_transport_port: 9042
```
## Data

In `cassandra.yaml`
```yaml
data_file_directories:
- /var/lib/cassandra/data
commitlog_directory: /var/lib/cassandra/commitlog
```

## Memory

https://cassandra.apache.org/doc/latest/cassandra/operating/hardware.html#memory

The Cassandra heap should be no less than 2GB, and no more than 50% of your system RAM


## Doc
* [SQL Ref](https://cassandra.apache.org/doc/cql3/CQL-2.2.html)
* [Rpm Package](https://cassandra.apache.org/doc/latest/cassandra/getting_started/installing.html#installing-the-rpm-packages)
* [Ansible Role](https://github.com/ansible-collections/community.cassandra)
