@echo on

SET SCRIPT_PATH=%~dp0
SET IMAGE_NAME=packaged-container-postgres
SET CONTAINER_NAME=postgres

docker stop %CONTAINER_NAME%
docker remove %CONTAINER_NAME%
docker build --target final -t %IMAGE_NAME% %SCRIPT_PATH%\..
docker run --env-file %SCRIPT_PATH%\..\secret.env --name %CONTAINER_NAME% -d -p 5434:5432 -p 9187:9187 -p 9399:9399 -v C:\temp\data:/data %IMAGE_NAME%
docker logs %CONTAINER_NAME%

