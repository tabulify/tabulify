# SQL Server Data Store


## Test Container 

Test container are created on the fly when testing but you can work with an already started one to avoid the start time.
See [test](../doc/Test.md)

  * Create the container

```cmd
docker run ^
    -e 'ACCEPT_EULA=Y'  `#Accept the license term` ^
    -e 'SA_PASSWORD=TheSecret1!' ^
    -p 1433:1433 ^
    -d ^
    --name sqlserver ^
    microsoft/mssql-server-linux:2017-latest
```

  * Start

```cmd
docker start sqlserver
```

## Connection Properties

[Connection Properties](https://docs.microsoft.com/en-us/sql/connect/jdbc/setting-the-connection-properties)
