# Bytle Db


## About

The db module containing:
  * the model
  * the data generator because:
      * the model tests used it and we don't want to introduce a cycle ie gen -> db and db -> gen
      * all other modules used it to test
      * Idea has an issue with that (https://github.com/mplushnikov/lombok-intellij-plugin/issues/161)


The top object is:
  * in the model: the database object and have no parent, all other object have a parent that lead to the top object (ie the database object)
  * in the stream model: the tableDef

## Circular Dependencies

You will get a circular dependency that if this module needs to use a library
that is also used in a `data path` module implementation.

For instance, `yaml` is used to read configuration file
but is also used as a `data path` in the `db-yaml` module.

In this case, you need to add in the `bytle-type` module:
  * the `yaml` library
  * the common class


## Object Hierarchy

RelationDef is the base object:

  * TableDef implements a SQL Relation
  * ...

## Model
  * A tableDef object is the entry point of the whole model.

A tableDef may have:

  * A database object is a wrapper around a connection and has a relation one to many schemas
  * A schemaDef object is created from a database object and has a relation one to many tables
  * Catalog is not yet supported

Creation:

  * A columnDef object is created from a targetTableDef object
  * A primaryKey object is created from a targetTableDef object
  * A foreignKey object is created from a targetTableDef object

The dataType object is the only independent object and comes from a collection created from the database.
Precision and scale are attributes of the column object.

### Stream

  * A stream is an table operation (Created from a tableDef ?)
  * A stream is always against a tableDef



## Note


### Features

  * Use of: https://maven.apache.org/surefire/maven-failsafe-plugin/index.html


## Library
### Jdbc

  * https://commons.apache.org/proper/commons-dbutils/

### Test

  * [Sqllogictest](https://www.sqlite.org/sqllogictest/doc/trunk/about.wiki) compares the results to identical queries from other SQL database engines
  * https://www.cockroachlabs.com/blog/testing-random-valid-sql-in-cockroachdb/
  * A random SQL query generator - [SqlSmith](https://github.com/anse1/sqlsmith)

## Todo

See bash completion, man:

  https://github.com/kennethreitz/legit/tree/develop/extra

## Concurrency

