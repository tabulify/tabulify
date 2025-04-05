# Sqlite Database implementation

## About

`Sqlite` is the reference SQL Database implementation.

## Type

When creating a table, you should use the well known type code and not the native Sqlite type code 

The driver maps the type code 12 (VARCHAR to TEXT).
 
## Test

Just FYI:
  * The official SQLite test are [here](https://www.sqlite.org/src/tree?ci=trunk&name=test)
  * The SQL were already parsed at [the sqlite-parser test project](https://github.com/gerardnico/sqlite-parser/tree/master/src/test/resources)

## View and Sqlite Versatility

  * View expression does not show any data type
  * Bad View may be stored
