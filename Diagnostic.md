# Diagnostic

## Debug Order

The following module must be tested in order:

  * [Db Core](./db)
  * [Db Sqlite](./db-sqlite) 
  * [Db Generation](./db-gen)

Ie if [Db Generation](./db-gen) got a problem, this may be caused by the [Db Sqlite](./db-sqlite) module giving bad metadata. 