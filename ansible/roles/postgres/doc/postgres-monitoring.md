## Disk Capacity

Disk capacity is monitored at regular intervals.
When capacity exceeds the pre-defined threshold of 90%,
every user-defined table will become read-only.
When disk usage falls below the defined threshold,
either through file cleanup or volume extension read/write will be re-enabled automatically.
https://github.com/fly-apps/postgres-flex/blob/master/docs/capacity_monitoring.md

They use:

```
SHOW default_transaction_read_only
```

[ReadOnly Code](https://github.com/fly-apps/postgres-flex/blob/master/internal/flypg/readonly.go)
[Check](https://github.com/fly-apps/postgres-flex/blob/f682f391774e96ded65e9a4bfc4698cfffaac953/internal/flycheck/pg.go#L17)
[Check Web Server](https://github.com/fly-apps/postgres-flex/blob/master/internal/api/handler.go)

```golang
for _, db := range databases {

    // exclude administrative dbs
    if db.Name == "repmgr" || db.Name == "postgres" {
      continue
    }

    sql := fmt.Sprintf("ALTER DATABASE %q SET default_transaction_read_only=%v;", db.Name, enable)
    if _, err = conn.Exec(ctx, sql); err != nil {
      return fmt.Errorf("failed to alter readonly state on db %s: %s", db.Name, err)
    }
}
```

## Graphana

https://github.com/fly-apps/postgres-flex/blob/master/grafanadash.example.json

## Database size

* [Object size function](https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-ADMIN-DBOBJECT)

```sql
select *
from pg_size_pretty(pg_database_size('dbname'));
-- pg_table_size, pg_indexes_size, ...
```
