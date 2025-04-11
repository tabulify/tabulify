# Dev

## How to run the actual cli

### With local Install

* Install it locally
```bash
task release-install
```
* Run it ([envrc](../../.envrc) adds it to the path)
```bash
tabli
```

### With fat jar

```bash
task install
java -jar "$SCRIPT_PATH"/target/tabulify-cli-0.1.0-SNAPSHOT.jar "$*"
```


## Docker Image

Build:

```bash
# Then
cd ..
docker build -f db-cli/Dockerfile -t ghcr.io/gerardnico/tabli .
# run
docker run --rm ghcr.io/gerardnico/tabli
```

Push:

```bash
# login with a github token
echo $DOCKER_GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
docker push ghcr.io/gerardnico/tabli
```



## Note
### Example of log for an execution

Example of execution log: https://www.nextflow.io/docs/latest/tracing.html#execution-log

### How to set the procname

* https://github.com/airlift/procname - Set process name for Java
