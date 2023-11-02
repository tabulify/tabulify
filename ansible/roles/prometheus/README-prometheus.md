# Prometheus


## About

Installation of the Node exporter as describe in this [page](https://prometheus.io/docs/guides/node-exporter/)

To do: [Basic Auth](https://prometheus.io/docs/guides/basic-auth/#nginx-example)

## Run

```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags prometheus
```

## TODO

For batch: https://github.com/prometheus/pushgateway

The Prometheus Pushgateway exists to allow ephemeral and batch jobs to expose their metrics to Prometheus.
  * It's a service running at:http:\\localhost:9091

See https://prometheus.github.io/client_java/

## Php-fpm

  * https://github.com/bakins/php-fpm-exporter
  * https://easyengine.io/tutorials/php/fpm-status-page/

## URL

  https://beau.bytle.net/prometheus
