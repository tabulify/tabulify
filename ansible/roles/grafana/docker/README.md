# Grafana


## Steps

### Docker

See [script](grafana-docker-run.cmd)

### Grafana

  * Go to [http://localhost:3000](http://localhost:3000)
  * Login: "admin" / "admin"
  * Data Source: Add a Prometheus Data Source. 
     * Configuration > Data Source > Prometheus > `http://host.docker.internal:9090`
  * Home (Top/Left Icon) > Import Dashboard:
     * [Node Exporter](https://grafana.com/grafana/dashboards/10180) - also available at [grafana-dashboard-linux.json](grafana-dashboard-linux.json)
     * [Wmi Exporter](https://grafana.com/grafana/dashboards/10171) - also available at [grafana-dashboard-windows.json](grafana-dashboard-windows.json)

## Ref / Doc 

  * https://prometheus.io/docs/visualization/grafana/
  * https://grafana.com/docs/grafana/latest/installation/configure-docker
  * https://grafana.com/grafana/download?platform=docker
