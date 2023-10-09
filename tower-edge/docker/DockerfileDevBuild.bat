SET SCRIPT_DIR=%~dp0
SET WORKING_DIR=%cd%
cd /D %SCRIPT_DIR%\..
call ..\gradlew assemble
docker build -t net.bytle/edge -f Dockerfile .
cd /D %WORKING_DIR%
