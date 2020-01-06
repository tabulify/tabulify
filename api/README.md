# Bytle APIs

## About
Web Service Endpoint

## Deployment

[Dev](../doc/Dev.md) setup to get the registry credentials then:
```
cd api
gradle clean test uploadShadow
cd ../ansible
ansible-playbook playbook-root.yml -i inventories/ovh-vps.yml --vault-id passphrase.sh --tags gnico
```

### Run

To run your application:

  * From Idea
    * Main Class: net.bytle.api.Launcher
    * Program Arguments: run net.bytle.api.MainVerticle
    * Working Dir: D:\code\bytle-mono\api (at the start, it will create a db and download the Ip data)


## Database Migration

  * database migration is taken into the code of the [DatabaseVerticle](src/main/java/net/bytle/api/db/DatabaseVerticle.java)
  * the source script are in the [resources directory](src/main/resources/db/migration/)

## Help

* [Vert.x Documentation](https://vertx.io/docs/)
* [Vert.x Stack Overflow](https://stackoverflow.com/questions/tagged/vert.x?sort=newest&pageSize=15)
* [Vert.x User Group](https://groups.google.com/forum/?fromgroups#!forum/vertx)
* [Vert.x Gitter](https://gitter.im/eclipse-vertx/vertx-users)
