# Test


## CI/CD

The CI test is performed on the master branch via [gitlab](GitLab.md).

## Test 

Managed by the [test module](../test/src/main/java/net/bytle/test/TestContainerWrapper.java)

  * For GitLab, see [doc](https://www.testcontainers.org/supported_docker_environment/continuous_integration/gitlab_ci/)

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
