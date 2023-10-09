SET SCRIPT_DIR=%~dp0
SET WORKING_DIR=%cd%
cd %SCRIPT_DIR%\..
docker build -t net.bytle/edge -f tower-edge/Dockerfile .
cd %WORKING_DIR%
