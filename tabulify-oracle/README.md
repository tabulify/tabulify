# Oracle



## Test Container

### Continuous Integration

https://github.com/gvenzl/setup-oracle-free

### Free vs Xe

They have the same size, it takes between 5 and 7 minutes
* xe: `INFO: Pull complete. 9 layers, pulled in 480s (downloaded 1 GB at 2 MB/s)`
* free: `INFO: Pull complete. 9 layers, pulled in 316s (downloaded 1 GB at 3 MB/s)`

* Free is oracle but only for a couple of resources (cpu, ...)
  * https://www.oracle.com/nl/database/free/
  * https://java.testcontainers.org/modules/databases/oraclefree/
  * https://hub.docker.com/r/gvenzl/oracle-free
  * https://github.com/gvenzl/oci-oracle-free
* Xe:
  * https://testcontainers.com/modules/oracle-xe/


We are using the docker express image
because the free image is too big.

```dos
docker run ^
    --name=oracle ^
    -d ^
    -p 1521:1521 ^
    -p 51521:1521 \
    -p 55500:5500 \
    -e ORACLE_PWD=mysecurepassword \
    -e ORACLE_CHARACTERSET=AL32UTF8 \
    -e ORACLE_ALLOW_REMOTE=true ^
    wnameless/oracle-xe-11g-r2
```

### Official

Container can also be build from:
   * [Single Instance](https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance)
     * [18c](https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance/dockerfiles/18.4.0)
   * https://blogs.oracle.com/oraclemagazine/deliver-oracle-database-18c-express-edition-in-containers

Example
```bash
git clone --depth 1 https://github.com/oracle/docker-images
cd OracleDatabase\SingleInstance\dockerfiles\18.4.0
docker build -t gerardnico:oracle:18.4.0 -f Dockerfile.xe .
```

### Driver

  * ojdbc8 means certified with jdk8
  * https://repo1.maven.org/maven2/com/oracle/database/jdbc/ojdbc8/
  * https://www.oracle.com/database/technologies/jdbc-upc-downloads.html

### Oracle

Not used to long, too big image.

The below text was let just for information.

[Link](https://container-registry.oracle.com/pls/apex/f?p=113:4:115223795065689::NO:4:P4_REPOSITORY,AI_REPOSITORY,AI_REPOSITORY_NAME,P4_REPOSITORY_NAME,P4_EULA_ID,P4_BUSINESS_AREA_ID:8,8,Oracle%20Database%20Standard%20Edition%202,Oracle%20Database%20Standard%20Edition%202,1,0&cs=3xO6d1T_Pz-DEcttQ99PWMOoAzQaut1tPTXCnqu952i0xk9xjncfrSrnJAfWtUSZYOqFLKA2vJMvlUqjHJwxDPA)

Steps:

  * Login
```dos
docker login container-registry.oracle.com
```
```txt
Username: nico@gerardnico.com
Password:
Login Succeeded
```

  * Run
```dos
docker run ^
    --name oracle ^
    -d ^
    -p 1521:1521 ^
    -p 5500:5500 ^
    -it ^
    container-registry.oracle.com/database/standard
```

## Oracle Type

  * [Oracle Type Constant](http://docs.oracle.com/cd/E11882_01/appdev.112/e13995/constant-values.html#oracle_jdbc_OracleTypes_BINARY_DOUBLE)
  * [Data Type Mapping - Database JDBC Developer's Guide and Reference](http://docs.oracle.com/cd/B28359_01/java.111/b31224/datacc.htm#i1008338)

Just backup info ...

```txt
//    BFILE(oracle.jdbc.OracleTypes.BFILE, JdbcDataType.BINARY.getDataTypeCategory(), null, oracle.sql.BFILE.class, "BFILE", false, false ),
//    CURSOR(oracle.jdbc.OracleTypes.CURSOR, JdbcDataTypeCategory.SPECIAL, java.sql.ResultSet.class, oracle.jdbc.OracleResultSet.class, "REF CURSOR", false, false   ),
//    TIMESTAMP_EXT(oracle.jdbc.OracleTypes.TIMESTAMP, JdbcDataType.TIMESTAMP.getDataTypeCategory(), JdbcDataType.TIMESTAMP.getJavaDataType(), oracle.sql.TIMESTAMP.class, "TIMESTAMP", false, false ),
//    TIMESTAMP_WITH_TIME_ZONE(oracle.jdbc.OracleTypes.TIMESTAMPTZ, JdbcDataType.TIMESTAMP.getDataTypeCategory(), JdbcDataType.TIMESTAMP.getJavaDataType(), oracle.sql.TIMESTAMP.class, "TIMESTAMP WITH TIME ZONE", false, false),
//    TIMESTAMP_WITH_LOCAL_TIME_ZONE(oracle.jdbc.OracleTypes.TIMESTAMPLTZ, JdbcDataType.TIMESTAMP.getDataTypeCategory(), JdbcDataType.TIMESTAMP.getJavaDataType(), oracle.sql.TIMESTAMP.class, "TIMESTAMP WITH LOCAL TIME ZONE", false, false),
//    BINARY_DOUBLE(OracleTypes.BINARY_DOUBLE, JdbcDataType.DATE.getDataTypeCategory(), JdbcDataType.DOUBLE.getJavaDataType(), oracle.sql.BINARY_DOUBLE.class, "BINARY_DOUBLE",false, false ),
//    BINARY_FLOAT(OracleTypes.BINARY_FLOAT, JdbcDataType.FLOAT.getDataTypeCategory(), JdbcDataType.FLOAT.getJavaDataType(), oracle.sql.BINARY_FLOAT.class, "BINARY_FLOAT",false, false ),
```


