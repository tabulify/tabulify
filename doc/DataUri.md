# Data Uri

## About

Syntax

```
[path]@datastore[?query][#fragment]
```
where:
  * `datastore` is the only mandatory part
  * `path` structure is data store dependent: ie
    * `catalog.schema.object` for sql
    * `schema.object` for sql
    * `../schema` for sql
    * `../../catalog` for sql
    * `object` for sql
    * `/foo/bar` for file

## Type Store

### File System (Hierarchical based)

  * /catalog/schema/table@fs
  * C:\catalog\schema\table@fs
        
### SQL 

  * Relative ../schema@fs?query=1&asd=2#element
  * Absolute: schema or catalog depending on the SQL database implementation
    * /schema@fs?query=1&asd=2#element
    * /catalog@fs?query=1&asd=2#element

In Sql the point is a path separator but as we want to be able to define an absolute path
the following syntax was not chosen (schema.table@fs?query=1&asd=2#element) 
because the point has meaning (one point is the current working path, `..` is the parent).

        