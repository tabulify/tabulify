# Data Path


## About 

A data path is the memory/code representation of a unique object inside a data store.

## Type

  * File
    * Csv
    * Json
    * Xml
    * Html (Web Page)
    * ...
  * Relational
    * Query
    * View
    * Table

## Scope (Lifecycle)

The scope is the data store.
Because the data store is a closeable object, when enclosing it in a try statement, 
the data path object should be deleted (ie got a null at the end).

Example: outside the `try` block, the `dataPath` variable is non-existent.
```java
try (MemoryDataStore memoryConnection = MemoryDataStore.of("test","test")){
      DataPath dataPath = memoryConnection.getDefaultDataPath("test")
        .getOrCreateDataDef()
        .addColumn("col1")
        .getDataPath();
}
```

## Name

If no name is given, the returned data path is the default datastore data path.
