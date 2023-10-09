# Deployment


## How to deploy ?

The deployment:
* create a fat jar
* upload it to the server
* starts and stops the [service on the server](server.md#service)


Steps:
* Create the file `~\.gradle\gradle.properties` with the user password
```ini
backendUserPwd = xxxxxxx
```
* then
```dos
cd tower
..\gradlew.bat clean test shadowJar release
..\gradlew.bat release # without test
```


## Before: installation

Before any deployment, the machine should be configured.  ie the ansible role should be run
to set the environment:
  * [files](env.md#production-deployment)
  * java and directory
