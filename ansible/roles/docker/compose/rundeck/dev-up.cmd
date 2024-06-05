set COMPOSE_PROJECT_NAME=eraldy
docker-compose -f compose.yml -f compose.dev.yml -f ..\core\compose.dev.yml up -d

REM Debug See the merged file
REM docker-compose -f compose.yml -f compose.dev.yml -f ..\core\compose.dev.yml config
