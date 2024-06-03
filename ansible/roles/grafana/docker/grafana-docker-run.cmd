@echo off
REM Create the grafana container from the grafana image

cd /D d:\temp
mkdir grafana
cd grafana

docker run  ^
    --name=grafana ^
    -d ^
    -p 3000:3000 ^
    -v %cd%:/var/lib/grafana ^
    grafana/grafana

