# Dev environment

## About
How to set up a dev environment

## Version

  * Gradle: 5.6.4

```
gradlew -v
```
```text
------------------------------------------------------------
Gradle 6.8.1
------------------------------------------------------------

Build time:   2021-01-22 13:20:08 UTC
Revision:     31f14a87d93945024ab7a78de84102a3400fa5b2

Kotlin:       1.4.20
Groovy:       2.5.12
Ant:          Apache Ant(TM) version 1.10.9 compiled on September 27 2020
JVM:          16.0.2 (Oracle Corporation 16.0.2+7-67)
OS:           Windows 10 10.0 amd64
```

The version is used in the [.gitlab-ci.yml file](../.gitlab-ci.yml)

## Nexus

Nexus is not supported anymore.
  * Create the file `~\.gradle\gradle.properties` with the nexus password
```ini
nexusPwd = xxxxxxx
```


## Java

As of the last version, Tabulify is:
  * built with 11
  * require at minimum Java 8 to run

We use the gradle toolchain to define them

## Idea

  * Download Idea
  * Create a project SDk:
    * Project Structure > Platform Settings > Sdk
    * Project Structure > Project Settings > Sdk 1.8, Level 8

Due to a cycle for test on db and db-gen, Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors.
See: https://github.com/mplushnikov/lombok-intellij-plugin/issues/161

Error:
```
Error:java: Annotation processing is not supported for module cycles. Please ensure that all modules from cycle [db,db-gen] are excluded from annotation processing
```



## Create the store location

To not use the name of the user in the path of the databases store, we set the environment variable to
`C:\Users\BytleDb\AppData\Local\db\databases.ini`

The permissions must be changed to authorize everybody to save data.


