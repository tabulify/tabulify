# Prometheus


## About

Installation of the Node exporter as describe in this [page](https://prometheus.io/docs/guides/node-exporter/)

To do: [Basic Auth](https://prometheus.io/docs/guides/basic-auth/#nginx-example)

## Run

```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags prometheus
```

## Php-fpm

  * https://github.com/bakins/php-fpm-exporter
  * https://easyengine.io/tutorials/php/fpm-status-page/

## URL

  https://beau.bytle.net/prometheus
