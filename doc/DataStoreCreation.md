# Data Store Extension



## Steps

  * Data Store: 
    * Create a data store object that extends:
       * the `AnsiDataStore` (Jdbc) 
       * or `FsDataStore` (File System)
    * Create a data store provider that will create this data store 
  * Data Path:
    * Create a data path object
    * Overwrite the data store functions:
       * `getSqlDataPath` (for an AnsiDataStore: `return new myDataPath(this, getCurrentCatalog(), getCurrentSchema(), null);`)
       * and `getQueryDataPath`
  * Data Def
    * Create a data def object, 
    * Overwrite the data store functions getOrCreateDataDef and createDataDef


  
