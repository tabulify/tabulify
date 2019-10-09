# Table System

## About
Two tables providers with sub implementation
  * jdbc
    * sql server
    * ...
  * file (csv, excel)
    * local
    * sftp,
    * http
    
```java
DataUri dataUri = DataUris.of("file://whatever.csv");
dataUri = DataUris.of("sftp://whatever.csv");
dataUri = DataUris.of("http://whatever.csv");
dataUri = DataUris.of("sqlite://schema/whatever");
RelationDef relationDef = Tabulars.get(dataUri);
```