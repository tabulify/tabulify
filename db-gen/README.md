# Data Generation


## Model

The [DataGeneration](./src/main/java/net.bytle.db/gen/DataGeneration.java) class is the main entry point of data generation where you will:

  * set up all data needed to create a generation.ie:
    * the tables to load with their total number of rows
    * the [DataGenerator](./src/main/java/net.bytle.db/gen/DataGenerator.java) which define the data generation for one or more columns
    * the load or not of a parent table (if a table has a parent table that is not included, the parent will be or not included)
  * start the load:
    * the load will then build default data generators for columns where the generator was not specified
    * and insert the rows

  
## Metadata

The data generation metadata may be given when:
  * building the data generator
  * or adding a table to the [DataGeneration](./src/main/java/net.bytle.db/gen/DataGeneration.java)
   
Furthermore, the [DataGeneration](./src/main/java/net.bytle.db/gen/DataGeneration.java) will try to find any missing information
in the property of the tables and columns (which is the case when the tableDef is created from a DataDefinition file).

## Example

The [testForeignKeyDataGenerationTest test](./src/test/java/net.bytle.db/gen/DataGenerationCodeTest.java) gives a good example on how to generate data.

```java
Integer totalRows = 3 * 365;
DataGeneration.of()
        .addTable(dimTable, totalRows)
        .addTable(factTable, totalRows)
        .addTable(dimCatTable)
        .addGenerator(dateIdGenerator)
        .addGenerator(monthNumberGemerator)
        .addGenerator(monthNameGenerator)
        .load();
```
 
## Documentation

  * https://en.wikipedia.org/wiki/Test_data_generation

## Data Generation

  * Data Generator. See [filler](https://www.cri.ensmp.fr/people/coelho/datafiller.html) [Code](https://www.cri.ensmp.fr/people/coelho/datafiller)
  * https://finraos.github.io/DataGenerator/
  * https://github.com/interana/eventsim#config-file
  * https://mockaroo.com/ - Data Type
  * https://github.com/mifmif/Generex
