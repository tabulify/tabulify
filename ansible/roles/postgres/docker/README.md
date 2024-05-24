## Other Docker setup

### Fly

* [HA setup using repmgr - primary and a standby server](https://github.com/fly-apps/postgres-flex)
* Old fly Postgress App: [HA setup using solon](https://github.com/fly-apps/postgres-ha) (old as
  stated [here](https://fly.io/docs/postgres/advanced-guides/high-availability-and-global-replication/))

### Supabase

https://github.com/supabase/postgres/blob/develop/Dockerfile

Fail2ban filter on the [README](https://github.com/supabase/postgres/) with fail2ban filter

## Extension

List:

* International Full Text Search: https://pgroonga.github.io/
* pgRouting: GeoSpatial routing calculation - https://pgrouting.org/
* [pg_plan_filter](https://github.com/pgexperts/pg_plan_filter): block execution of statements where query planner's
  estimate of the total cost exceeds a threshold.
* [pg_graphql](https://supabase.github.io/pg_graphql/) -
  * Each table receives an entrypoint in the top level Query type that is a pageable collection with relationships
    defined by its foreign keys.
  * Tables similarly receive entrypoints in the Mutation type that enable bulk operations for insert, update, and
    delete.
* PostGis:

```Dockerfile
apt-get install --no-install-recommends -y \
  postgresql-${PG_MAJOR}-postgis-${POSTGIS_MAJOR} \
  postgresql-${PG_MAJOR}-postgis-${POSTGIS_MAJOR}-scripts
```
## Extension Framework

* https://github.com/pgcentralfoundation/pgrx - framework for developing PostgreSQL extensions in Rust

## Checks

* UTF-8

