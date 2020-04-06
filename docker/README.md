# Prometheus


## About
Prometheus in docker in order to monitor the services.

## Steps

  * Run [prometheus-docker-run.cmd](prometheus-docker-run.cmd)
  * You get a container called `prom`
  * You can stop it
```dos
docker stop prom
```
  * You can start it
```dos
docker start prom
```
  * You can get access to Prometheus at [http://localhost:9090](http://localhost:9090)
  * Target should be up [http://localhost:9090/targets](http://localhost:9090/targets)

## Ref Doc

  * [Installation using docker](https://prometheus.io/docs/prometheus/latest/installation/#using-docker)
  * [Ansible Role](https://github.com/cloudalchemy/ansible-prometheus)
