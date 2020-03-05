# Data Store Extension



## Steps

  * Data Store: 
    * Create a data store object that extends:
       * the `SqlDataStore` (Jdbc) 
       * or `FsDataStore` (File System)
    * Create a data store provider that will create this data store 
  * Data Path:
    * Create a data path object
    * Overwrite the data store functions:
       * for a `SqlDataStore`
         * `getSqlDataPath` (for an AnsiDataStore: `return new myDataPath(this, getCurrentCatalog(), getCurrentSchema(), null);`)
         * `getQueryDataPath`
       * for a `FsDataStore`
         * `getFsDataPath`
       * for a memory data store
         * `getTypedDataPath` is the main entry for data path creation
  * Data Def
    * Create a data def object, 
    * Overwrite the data store functions getOrCreateDataDef and createDataDef


  
## Support

  * [Looker](https://docs.looker.com/setup-and-management/database-config)
  
