# Ip Web App


## About

This is a vertx web app API that respond with Ip geolocation data.

## Status

This project is not deployed yet.

## How to use

* if you change the [open Api file](src/main/openapi/ip-openapi.yaml)
```bash
# it will generate a indepedent spec file into the resource file for easy deployment
..\gradlew openapi
```
* You can start the app with the main method of the [IpVerticle](src/main/java/net/bytle/ip/IpVerticle.java)
* The openapi doc will be at: http://localhost:8084/ip/doc/
