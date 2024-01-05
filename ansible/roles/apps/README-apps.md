# Apps

## About
This role will do preparation works for the installation of the apps:
* tower: Api
* tabli: Tabulify
* inbox: Smtp

## Run

```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags apps
```

For one app:
```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags app-tower
#
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags app-inbox
#
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags app-tabli
```

## Appctl

you can control the app with the [appctl](./script/appctl) script.
