# Bytle APIs

## About
Web Service Endpoint

### Run

To run your application:

  * from gradle

```
cd api
gradle clean run
```

  * From Idea
    * Main Class: net.bytle.api.Launcher
    * Program Arguments: run net.bytle.api.MainVerticle
    * Working Dir: D:\code\bytle-mono\api (at the start, it will create a db and download the Ip data)

## Building

To launch your tests:
```
./gradlew clean test
```


To package your application:
```
./gradlew clean assemble
```



## Database Migration

  * database migration is taken into the code of the DatabaseVerticle
  * the source script are in the resources

## Help

* [Vert.x Documentation](https://vertx.io/docs/)
* https://stackoverflow.com/questions/tagged/vert.x?sort=newest&pageSize=15[Vert.x Stack Overflow]
* https://groups.google.com/forum/?fromgroups#!forum/vertx[Vert.x User Group]
* https://gitter.im/eclipse-vertx/vertx-users[Vert.x Gitter]
