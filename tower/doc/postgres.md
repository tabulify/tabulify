# Postgres


## About
We use postgres version `15.1`.

## Doc

  * For database migration, see [scheme](schema.md)

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
