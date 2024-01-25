# Ip Web App


## About

This is a vertx web app API that respond with Ip geolocation data.

## Status

This project is not deployed yet.

## How to use

* If you change the [open Api file](src/main/openapi/ip-openapi.yaml)
```bash
# it will generate a indepedent spec file into the resource file for easy deployment
..\gradlew openapi --info
```
* You can start the app with the main method of the [IpVerticle](src/main/java/net/bytle/ip/IpVerticle.java)
* The openapi doc will be at: http://localhost:8084/ip/doc/

## SQl

How to select with the string representation:
```sql
select *
from (select '0.0.0.0'::inet + ip_from AS ip_from_inet,
             '0.0.0.0'::inet + ip_to   AS ip_to_inet,
             *
      FROM cs_ip.ip) ip_inet
where '142.176.206.81'::inet > ip_from_inet
  and '142.176.206.81'::inet < ip_to_inet
```
