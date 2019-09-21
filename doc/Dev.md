# Dev environment

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

## Idea

Due to a cycle for test on db and db-gen, Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors.
See: https://github.com/mplushnikov/lombok-intellij-plugin/issues/161

Error:
```
Error:java: Annotation processing is not supported for module cycles. Please ensure that all modules from cycle [db,db-gen] are excluded from annotation processing
```


## Bytle Db Dev doc

See [doc](db-cli/src/doc/README.md)

## Create the store location

To not use the name of the user in the path of the databases store, we set the environment variable to
`C:\Users\BytleDb\AppData\Local\db\databases.ini`

The permissions must be changed to authorize everybody to save data.
