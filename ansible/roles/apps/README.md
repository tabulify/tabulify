# Apps

## About
This role will do preparation works for the installation of the apps:
* tower (vertx java fat jar)
* tabli

## Run

```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags apps
```

For one app:
```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags app-tower
```


