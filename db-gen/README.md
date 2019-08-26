# Data Generation


## Model

  * A [DataGeneration](./src/main/java/net.bytle.db/gen/DataGeneration.java) contains one or more [DataGenTableDef](./src/main/java/net.bytle.db/gen/DataGenDef.java) 
  * A [DataGenTableDef](./src/main/java/net.bytle.db/gen/DataGenDef.java) contains one or more [DataGenColumnDef](./src/main/java/net.bytle.db/gen/DataGenDefColumnDef.java)
  * A [DataGenColumnDef](./src/main/java/net.bytle.db/gen/DataGenDefColumnDef.java) contains one [DataGenerator](./src/main/java/net.bytle.db/gen/DataGenerator.java)
  * A [DataGenColumnDef](./src/main/java/net.bytle.db/gen/DataGenDefColumnDef.java) contains one [DataGenerator](./src/main/java/net.bytle.db/gen/DataGenerator.java)
  * A [DataGenerator](./src/main/java/net.bytle.db/gen/DataGenerator.java) contains one or more [DataGenColumnDef](./src/main/java/net.bytle.db/gen/DataGenDefColumnDef.java)  


## Documentation

  * https://en.wikipedia.org/wiki/Test_data_generation

## Data Generation

  * Data Generator. See [filler](https://www.cri.ensmp.fr/people/coelho/datafiller.html) [Code](https://www.cri.ensmp.fr/people/coelho/datafiller)
  * https://finraos.github.io/DataGenerator/
  * https://github.com/interana/eventsim#config-file
  * https://mockaroo.com/ - Data Type
  * https://github.com/mifmif/Generex
