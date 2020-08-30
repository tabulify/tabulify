# Dev environment

## About
How to set up a dev environment

## Version

  * Gradle: 5.6.4
  
```
gradlew -v
```
```text
Gradle 5.6.4

Build time:   2019-11-01 20:42:00 UTC
Revision:     dd870424f9bd8e195d614dc14bb140f43c22da98

Kotlin:       1.3.41
Groovy:       2.5.4
Ant:          Apache Ant(TM) version 1.9.14 compiled on March 12 2019
JVM:          1.8.0_251 (Oracle Corporation 25.251-b08)
OS:           Windows 10 10.0 amd64
```

## Nexus

  * Create the file `~\.gradle\gradle.properties` with the nexus password
```ini
nexusPwd = xxxxxxx
```


## Java 

Download the last JDK8 (Java SE Development Kit)

In the `~\.gradle\gradle.properties` add its path

```ini
org.gradle.java.home=C:/java/jdk1.8.0_231
```

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


## Others
###  Maven
Just FYI, maven was deprecated

  * Create the local repository at C:\Maven\repository (to not have it in your profile ${user.home}/.m2/repository)
  * Create a symlink to the settings configuration file repository
```dos
mklink /D C:\Users\gerardnico\.m2 C:\Users\gerardnico\Dropbox\config\Maven\.m2
```

  * Build

From idea
```bash
mvn clean install
```

