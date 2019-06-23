# JDBC Driver Management


## Hive Example

See:
  * [pom.xml of Hive](hive.xml)
  * Code

```dos
REM the Java home is neeed to find the tools.jar (which is a dependency)
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_191\bin
mvn --file hive.xml -B dependency:copy-dependencies -DoverWriteReleases=false -DoverWriteSnapshots=false -DoverWriteIfNewer=true -DoutputDirectory=C:\code-drivers\hive
```