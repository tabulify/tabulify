REM Command
REM https://docs.docker.com/compose/environment-variables/envvars/
set COMPOSE_PROJECT_NAME=eraldy
docker-compose -f compose.yml -f compose.dev.yml up -d

REM Debug See the merged file
REM docker-compose -f compose.yml -f ..\compose.dev.yml config
