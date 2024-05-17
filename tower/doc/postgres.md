# Postgres


## About
We use postgres version `15.1`.

## Pool and timeout

A timeout may come when there is no connection available in the pool
if the connections are not released or busy.

Connection needs to be:
  * released (ie closed)
  * or get with the `pool.withConnection` function

You can check it:
* on Postgres side with (For Eraldy Api)
```sql
select * from pg_stat_activity where application_name like '%raldy%'  and state = 'active'
```
* on Vertx with the metrics `vertx_sql_queue_pending` ?

### Timeout Bug
In 4.4.8, I have some connection timeout created by this [line](https://github.com/eclipse-vertx/vertx-sql-client/blob/a2d2f9002a5fce562c8236c3310faad98038bb0d/vertx-sql-client/src/main/java/io/vertx/sqlclient/impl/pool/SqlConnectionPool.java#L219)
but I have the default setting (30 seconds).

Don't do `pool.query` ?
https://github.com/eclipse-vertx/vertx-sql-client/issues/1232

## Doc

  * For database migration, see [scheme](schema-flyway.md)

## Dev Install

### Docker

To get a local postgres with docker, you can run the below command.


```bash
docker run \
    --name tower \
    -e POSTGRES_PASSWORD=welcome \
    -p 5433:5432 \
    -d \
    postgres:15.1
```
```dos
docker run ^
    --name tower ^
    -e POSTGRES_PASSWORD=welcome ^
    -p 5433:5432 ^
    -d ^
    postgres:15.1
```
Note that the default port `5432` is mapped to `5433` to be able to run two different postgres database locally.
If you want to change it to your needs, the port is in the [tower config file](../.tower.yml)

then
```bash
docker start tower
docker stop tower
```

### Conf
Then run the following [sql file](../../ansible/roles/postgres/templates/db-conf.sql) to load the hstore extension.

## Native Client

We use the [Native client](https://vertx.io/docs/vertx-pg-client/java/)
that:
* can use Postgres built-in syntax such as `RETURNING`.
* [maps vertx object (json) to Postgres object](https://vertx.io/docs/vertx-pg-client/java/#_postgresql_type_mapping)
