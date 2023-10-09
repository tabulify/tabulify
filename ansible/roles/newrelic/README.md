# Installation of new relic

## About

This role is derived from [Anisble Agent](https://github.com/newrelic/infrastructure-agent-ansible/)



## Run

```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags newrelic
```

## Status

```bash
systemctl  status newrelic-infra
```

