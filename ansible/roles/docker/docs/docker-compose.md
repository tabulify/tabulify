## Delete a compose

https://docs.docker.com/reference/cli/docker/compose/rm/

```bash
docker compose rm -f project-name
```

## See the merged compose file

See the merged file with `config` at the end

Example:

```bash
docker-compose -f compose.yml -f ..\compose.dev.yml config
```
