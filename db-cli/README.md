# Db Cli


## Dev

To run the actual cli, use the [tabcli Gradle Wrapper](tabli.cmd)

## Utility

  * https://github.com/airlift/procname - Set process name for Java

## Gradle

  * [StartScript](https://docs.gradle.org/current/dsl/org.gradle.jvm.application.tasks.CreateStartScripts.html)

## Release / Deploy

  * Create the file `~\.gradle\gradle.properties` with:
```ini
backendServerHost = xxxxxxx
backendServerPort = xxxxxxx
appsUserName = apps
appsUserPwd = xxxxxxx
```
  * then release (create the distributions)
```
cd db-cli
..\gradlew clean test release
```
Deploy (upload to the server)
```
cd db-cli
..\gradlew deploy
```

## Install Locally

The `installDist` will install it locally at `${buildDir}\install`


## Keep

Example of execution log: https://www.nextflow.io/docs/latest/tracing.html#execution-log
