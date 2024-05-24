# Postgres

```
docker run --env-file secret.env --rm -it postgres_final
```

* pgdata - /var/lib/postgresql/data

## Init

* Database initialization only happens on container startup
* Run if you start the container with a data directory that is empty.

The (*.sql, *.sql.gz, or *.sh) init scripts

* should be located in the directory `/docker-entrypoint-initdb.d`
* are executed in sorted name order as defined by the current locale (default to en_US.utf8)

