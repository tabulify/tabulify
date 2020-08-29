# Test



## Test container

By default, the test container will be started, run and stop
When developing, this can lead to a lot of wait time.

For the project that supports it (Example: [sqlServer](../db-sqlserver/src/test/java/net/bytle/db/sqlserver/SqlServerTest.java), you can then create a file `~\.bytle.proeprties` 
and add the following properties to avoid that.

```ini
container.start=false
```


## Reactor

```powershell
Note that previous editions of this guide suggested to use <classifier>tests</classifier> instead of <type>test-jar</type>. 
While this currently works for some cases, 
it does not properly work during a reactor build of the test JAR module 
and any consumer if a lifecycle phase prior to install is invoked. 
In such a scenario, Maven will not resolve the test JAR from the output of the reactor build 
but from the local/remote repository. 
```

## Parameters
  * Just pass a parameter dommert !
  * [Junit - parameterized-suite](https://github.com/PeterWippermann/parameterized-suite)
  * [TestMG - Passing parameters to the whole test](https://www.seleniumeasy.com/testng-tutorials/parameterization-in-testng)

## Dependency Injection

See [Guice](https://github.com/google/guice/wiki/ModulesShouldBeFastAndSideEffectFree)

## Doc

  * http://maven.apache.org/guides/mini/guide-attached-tests.html
  * https://maven.apache.org/plugins/maven-jar-plugin/examples/create-test-jar.html
