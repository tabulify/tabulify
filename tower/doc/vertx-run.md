# How to start the Vertx API server?

## About
This page shows you how you can start the vertx API Webserver (ie backend)

## Principles: Two ways

> If you don't want to read the principles, you can also go directly to the [hands-on instructions](#how-to-run).

There is two ways to start a vertx instance:
* via the [vertx Launcher](#vertx-launcher) for production, command line and hot reload
* directly via a standard [main class](#main-class) for development from idea.

### Vertx Launcher
The vertx Launcher is the main entry to start Vertx at the command line.

We have a custom [Vertx Launcher class](../../vertx/src/main/java/net/bytle/vertx/MainLauncher.java).

It's used:
* in the vertx cli
* in our fat jar as main class (check the fat jar gradle task)

It takes as argument:
* our custom launcher
* our Main verticle
* hot reload option

Note: the [start, stop and list commands](https://vertx.io/docs/vertx-core/java/#_other_commands)
works only with a fatjar.

### Main Class

Via the [main method](../src/main/java/net/bytle/tower/VerticleApi.java),
you can start it easily from the idea. Locate the `main` function, right click and start with or without debug.

## How to run
### Prerequisites

* Databases: A [postgres instance](postgres.md) should be started.


### How to run it locally Without debug

To run your application:

From Idea:
* Main Class: [verticleAPI](../src/main/java/net/bytle/tower/VerticleApi.java)
* Working Dir: `D:\code\bytle-mono\tower`

From Idea with the main launcher
* Main Class: [net.bytle.vertx.MainLauncher](../../vertx/src/main/java/net/bytle/vertx/MainLauncher.java)
* VMOption: `-Denv=development`
* Program Arguments: `run net.bytle.tower.VerticleApi` [net.bytle.tower.VerticleApi](../src/main/java/net/bytle/tower/VerticleApi.java)
* Working Dir: `D:\code\bytle-mono\tower` (at the start, it will create a db and download the Ip data)

From Gradle:
```bash
..\gradlew runTower
```


### Hot Reload

Hot reload will reload for every change in the file with a debounce time.
If you don't this behavior, you can also easily restart the server with idea:
* Shift+F10: will restart in normal mode
* Shift+F9: will restart in debug mod

Otherwise, you can start with automatic reload
  * From Idea, run with debug:
     * Main Class: [net.bytle.tower.Main](../src/main/java/net/bytle/tower/Main.java)
     * Working Dir: `D:\code\bytle-mono\tower`
  * From Gradle:
```bash
..\gradlew runTowerHotReload
```
  * From the console: [tower.cmd](../tower.cmd) will run the `runTowerHotReload` gradle task

### With Debug

From a HTTP request:
* Start a Main Class ([net.bytle.tower.Main](../src/main/java/net/bytle/tower/VerticleApi.java) or [net.bytle.vertx.MainLauncher](../../vertx/src/main/java/net/bytle/vertx/MainLauncher.java)) in debug mode
* Put your break point
* Create a request with the browser
* Idea should stop on the break point

From a test:
* Put your break point
* Start the test in Debug mode

vertxDebug ?


## Gradle

In gradle, the tasks are in the group `runTower`
