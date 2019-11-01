# Maven


## About

Mvn command frequently used (cheat sheet)

## Install

Install without test
```bash
mvn clean install  -DskipTests
```

## Resume
Resume after a test fail - `rf` stands from `resume from`
 
```bash
mvn clean install -rf "module-directory-name" 
```
Example:
```bash
mvn clean install -rf "db-loader" 
```

## Until

```bash
mvn install --also-make --projects "csv" 
```
where:
  * -pl, --projects: Build specified reactor projects instead of all projects
  * -am, --also-make: If project list is specified, also build projects required by the list
