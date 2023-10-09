# Gradle Daemon


## About

The daemon is used when:
* starting an application in normal or debug mode
* starting the test in normal or debug mode

## See

```cmd
..\gradlew --status
```

## Configuration
* Gradle: org.gradle.daemon.* in [gradle.properties](https://docs.gradle.org/current/userguide/build_environment.html)
```properties
# by default 3 hour
org.gradle.daemon.idletimeout=10800000
```
*

## Log
```
~/.gradle/daemon/7.5.1/daemon-XXXX.out
```
where xxxx is the id given by the

## Shutdown

The daemon compete when running the application and the test

If you check the status, you get:
```
other compatible daemons were started and after being idle for 0 minutes and not recently used
```

In the log,
```
2023-10-09T10:01:30.282+0200 [LIFECYCLE] [org.gradle.launcher.daemon.server.DaemonStateCoordinator] Daemon will be stopped at the end of the build other compatible daemons were started and after being idle for 0 minutes and not recently used
2023-10-09T10:01:30.282+0200 [DEBUG] [org.gradle.launcher.daemon.server.DaemonStateCoordinator] Marking daemon stopped due to other compatible daemons were started and after being idle for 0 minutes and not recently used. The daemon is running a build: false
2023-10-09T10:01:30.283+0200 [DEBUG] [org.gradle.launcher.daemon.server.DaemonStateCoordinator] daemon has stopped.
2023-10-09T10:01:30.283+0200 [DEBUG] [org.gradle.launcher.daemon.server.Daemon] stop() called on daemon
2023-10-09T10:01:30.283+0200 [INFO] [org.gradle.launcher.daemon.server.Daemon] Stop requested. Daemon is removing its presence from the registry...
```

The doc on [compatibility](https://docs.gradle.org/current/userguide/gradle_daemon.html#deamon_compatibility)
* Gradle starts a new Daemon if no idle or compatible Daemons exist.

One solution is to set: at the command line ?
```
-Djava.io.tmpdir=C:\temp\idearun
```
