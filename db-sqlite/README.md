# Sqlite Database implementation

## About

`Sqlite` is the reference SQL Database implementation.

## Type

When creating a table, you need to use the well known type code and not the native Sqlite type code 

(ie TEXT is not supported).

The driver maps the type code 12 (VARCHAR to TEXT).
 