# Bytle Db


## About

The core module containing:
  * the model
  * the data generator because:
      * the model tests used it and we don't want to introduce a cycle ie gen -> db and db -> gen
      * all other modules used it to test
      * Idea has an issue with that (https://github.com/mplushnikov/lombok-intellij-plugin/issues/161)

## Design

  * A function may return null if a variable was never set
  * A getOf function never return null. It does a get and a create an object if it does not exist). 
  * The Object.getOf or StaticClass.get function is the single point of object creation.
  * A get function may return null to be consistent with the Java collection get function
  * A set function handle always null and never return an exception for a null value
  * The default values are returned in the get function and are never set on the object property to be able to know if the value was set or not.
  * We are never mixing differents structure (table, index, query, ...) in function arguments. Even if a string may be a query or a table name, we are expecting only one of the two.
  * Meta above data. Example: Insert statement modified in place of switching the columns
  * The name of an object gives its location (a file, ...)


The top object is:
  * in the model: the database object and have no parent, all other object have a parent that lead to the top object (ie the database object)
  * in the stream model: the tableDef

## Object Hierarchy

RelationDef is the base object:

  * TableDef implements a SQL Relation
  * ...

### Model
  * A database object is a wrapper around a connection
  * A schemaDef object is created from a database object
  * A targetTableDef object is created from a database object
  * A columnDef object is created from a targetTableDef object
  * A primaryKey object is created from a targetTableDef object
  * A foreignKey object is created from a targetTableDef object

The dataType object is the only independent object and comes from a collection created from the database.
Precision and scale are attributes of the column object.

### Stream

  * A stream is an table operation (Created from a tableDef ?)
  * A stream is always against a tableDef  



## Note 

### Data Generation

  * https://en.wikipedia.org/wiki/Test_data_generation
  * Data Generator. See [filler](https://www.cri.ensmp.fr/people/coelho/datafiller.html) [Code](https://www.cri.ensmp.fr/people/coelho/datafiller)
  * https://finraos.github.io/DataGenerator/

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

    