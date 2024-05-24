# Extension

## List

* International Full Text Search: https://pgroonga.github.io/
* pgRouting: GeoSpatial routing calculation - https://pgrouting.org/
* [pg_plan_filter](https://github.com/pgexperts/pg_plan_filter): block execution of statements where query planner's
  estimate of the total cost exceeds a threshold.
* [pg_graphql](https://supabase.github.io/pg_graphql/) -
  * Each table receives an entrypoint in the top level Query type that is a pageable collection with relationships
    defined by its foreign keys.
  * Tables similarly receive entrypoints in the Mutation type that enable bulk operations for insert, update, and
    delete.
* [PostGis](https://github.com/postgis/docker-postgis/blob/81a0b55/14-3.2/Dockerfile):

```Dockerfile
apt-get install --no-install-recommends -y \
  postgresql-${PG_MAJOR}-postgis-${POSTGIS_MAJOR} \
  postgresql-${PG_MAJOR}-postgis-${POSTGIS_MAJOR}-scripts
```

## Extension Framework

* https://github.com/pgcentralfoundation/pgrx - framework for developing PostgreSQL extensions in Rust

## List

```bash
psql
\dx
```

```
                 List of installed extensions
Name   | Version |   Schema   |         Description
---------+---------+------------+------------------------------
plpgsql | 1.0     | pg_catalog | PL/pgSQL procedural language
```

## Contrib

The [contrib package](https://www.postgresql.org/docs/14/contrib.html) has most of the extensions and should be
installed

## Extensions List

### Hstore

[Hstore](https://www.postgresql.org/docs/current/hstore.html)

Example: [Assign to NEW by key in a Postgres trigger](https://dba.stackexchange.com/questions/82039/assign-to-new-by-key-in-a-postgres-trigger/82044#82044)
