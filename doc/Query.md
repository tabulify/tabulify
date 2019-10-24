# Query

## About

A query is seen as:
   * path without a name. 
   * a data definition but at runtime.
   * a view without name

 
It permits us to do thing like:

```java
SelectStream sourceSelectStream = Tabulars.getSelectStream(queryDataPath);
SelectStream sourceSelectStream = Tabulars.getSize(queryDataPath);
```

The advantages:
  * The method signature does not change. Therefore the code is then easier before all to move data around.
  * As a view is a valid data path, a query can be seen as a view without name

The internal code becomes difficult
  * The internal code needs to take it into account



