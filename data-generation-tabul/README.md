# Data Generation


## Model

The [DataGenerator](./src/main/java/com/tabulify/gen/DataGenerator.java) class is the main entry point of data generation where you will:

  * set up all data needed to create a generation.ie:
    * the tables to load with their total number of rows
    * the load or not of a parent table (if a table has a parent table that is not included, the parent will be or not included)
  * start the load:
    * the load will then build default data generators for columns where the generator was not specified
    * and insert the rows


## Metadata

The data generation metadata is given when:
  * building the [gen data path](./src/main/java/com/tabulify/gen/GenDataPath.java))
  * or adding a table to the [DataGenerator](./src/main/java/com/tabulify/gen/DataGenerator.java)

Furthermore, the [DataGenerator](./src/main/java/com/tabulify/gen/DataGenerator.java) will try to find any missing information in the property of the tables and columns (which is the case when the tableDef is created from a DataDefinition file).

## Example



```java
Integer totalRows = 3 * 365;
DataGenerator.create(tabular)
        .addDummyTransfer(dimTable, totalRows)
        .addDummyTransfer(factTable, totalRows)
        .addDummyTransfer(dimCatTable)
        .load();
```

## Documentation

  * https://en.wikipedia.org/wiki/Test_data_generation

## Library

  * [java-faker](https://github.com/DiUS/java-faker) (TODO?)

## Data Generation

  * Data Generator. See [filler](https://www.cri.ensmp.fr/people/coelho/datafiller.html) [Code](https://www.cri.ensmp.fr/people/coelho/datafiller)
  * https://finraos.github.io/DataGenerator/
  * https://github.com/interana/eventsim#config-file
  * https://mockaroo.com/ - Data Type
  * https://github.com/mifmif/Generex
  * [SQlite generate_series extension table](https://www.sqlite.org/vtab.html#tabfunc2)
