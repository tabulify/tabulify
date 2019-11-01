# Dev environment


## Maven


  * Create the local repository at C:\Maven\repository (to not have it in your profile ${user.home}/.m2/repository)
  * Create a symlink to the settings configuration file repository
```dos
mklink /D C:\Users\gerardnico\.m2 C:\Users\gerardnico\Dropbox\config\Maven\.m2
```

## Java 
Download the last JDK8 (Java SE Development Kit)
    
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

## Build

From idea
```bash
mvn clean install
```

## Cli

After the first build:
  * create a `bin` directory
```
C:\Users\userName\bin
```
  * add it to your path
  * create a symlink of the following file in it
```
ROOT_DIR\db-cli\target\btyle-db-cli-1.1.0-SNAPSHOT\db.bat"
```
  * then when you call `db` you will get the last build library



## Bytle Db Dev doc

See [doc](../db-cli/src/doc/README.md)

## Create the store location

To not use the name of the user in the path of the databases store, we set the environment variable to
`C:\Users\BytleDb\AppData\Local\db\databases.ini`

The permissions must be changed to authorize everybody to save data.
