REM fly secrets set edge_s3_access_key_id=xxxx
REM fly secrets set edge_s3_access_key_secret=xxxx
SET SCRIPT_DIR=%~dp0
SET WORKING_DIR=%cd%
cd /D %SCRIPT_DIR%\..
call ..\gradlew assemble
docker build -t net.bytle/edge -f Dockerfile .
fly deploy --local-only
REM fly logs -a toweredge
