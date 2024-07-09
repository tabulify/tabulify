# Db Cli


## Dev

To run the actual cli, use the [tabcli Gradle Wrapper](tabli.cmd)

## Utility

  * https://github.com/airlift/procname - Set process name for Java

## Gradle

  * [StartScript](https://docs.gradle.org/current/dsl/org.gradle.jvm.application.tasks.CreateStartScripts.html)

## Docker Image

Build:

```bash
# Then
cd ..
docker build -f db-cli/Dockerfile -t ghcr.io/gerardnico/tabli:1.0.0 .
# run
docker run --rm ghcr.io/gerardnico/tabli:1.0.0
```

Push:

```bash
# login with a github token
echo $DOCKER_GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
docker push ghcr.io/gerardnico/tabli:1.0.0
```

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
