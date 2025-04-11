# Name of Object

## Rules

  * In the code, don't use the `-` minus because it has a meaning in SQL ie (`A-B` is a formula in SQL)
  * Key name:
     * must be written in the code:
        * lowercase
        * with the `_` such as `logical_name` as separator
     * can be written in the configuration file:
       * CamelCase as `logicalName`
       * and are case independent

## Key Normalizing

When matching two keys, the [normalize function](../db/src/main/java/net/bytle/db/spi/Key.java) should be used  
