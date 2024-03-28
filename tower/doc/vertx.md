# Vertx

## About

Vertx is a java web server reactive framework that supports tcp and http connection.

## Platform

It's our platform.

It includes:
* Jackson
* Guava
* Netty


## Help

* [Vert.x example](https://github.com/vert-x3/vertx-examples/tree/master/web-examples/src/main/java/io/vertx/example/web)
* [Vert.x Documentation](https://vertx.io/docs/)
* [Vert.x Stack Overflow](https://stackoverflow.com/questions/tagged/vert.x?sort=newest&pageSize=15)
* [Vert.x User Group](https://groups.google.com/forum/?fromgroups#!forum/vertx)
* [Vert.x Gitter](https://gitter.im/eclipse-vertx/vertx-users)

## Architecture

Based on [Netty](https://netty.io/) - event loop and so. See [Wiki](https://netty.io/wiki/index.html)

## Context

A context determine in which mode, the handlers are executed (event loop, worker, ...).

A verticle starts and has only one context of execution (by default the event loop)
Context is then also used as synonym for a verticle, even if you can execute on another context
via worker.

## Blocking code

By default, if executeBlocking is called several times from the same context
(e.g. the same verticle instance or worker), then the different executeBlocking
are executed serially (i.e. one after another).


## onFailure


When a failure occurs, there is only two choices:
  * report to the logger with the `onFailure` function (that should also log the location of the error).
  * returns another value with the `otherwise` function

There is no way to wrap the error and to propagate it (return it as failed future) as the `otherwise`
methods expect a returned value and not a failed future.

[Thread](https://groups.google.com/g/vertx/c/ui5djawjmRE/m/RProZKIpAwAJ)


## NoStackTraceThrowable

`NoStackTraceThrowable` are failed future created without any exceptions.

Example: if you get a `NoStackTraceThrowable: Timeout`, it was created with
```
Future.failedFuture("Timeout")
```


## Sql Database Timeout ???

In 4.4.8, I have some connection timeout created by this [line](https://github.com/eclipse-vertx/vertx-sql-client/blob/a2d2f9002a5fce562c8236c3310faad98038bb0d/vertx-sql-client/src/main/java/io/vertx/sqlclient/impl/pool/SqlConnectionPool.java#L219)
but I have the default setting (30 seconds).

Don't do `pool.query`
https://github.com/eclipse-vertx/vertx-sql-client/issues/1232

## Dev Mode

For vertx, the Dev mode is :
* with the VERTXWEB_ENVIRONMENT environment variable
* or vertxweb.environment system property
set to dev.
