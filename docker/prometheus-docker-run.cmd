@echo off
REM Create the prom container from the prometheus image

SET SCRIPT_PATH=%~dp0
cd %SCRIPT_PATH%

docker run ^
  --name prom ^
  -d ^
  -p 9090:9090 ^
  -v %cd%:/etc/prometheus ^
  prom/prometheus ^
  --config.file /etc/prometheus/prometheus.yml ^
  --web.enable-lifecycle
REM web.enable-lifecycle is to enable reload of the config file
REM when web.enable-lifecycle is enabled, you need to pass the config.file parameter
REM Ref: https://github.com/prometheus/prometheus/blob/master/docs/configuration/configuration.md
