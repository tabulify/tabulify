# How to add a new domain and its mail


## Steps

  * Go to the configuration file [virtual](./defaults/main/dkim.yml)
  * Add the domain
  * Add the alias
  * Run the role
```bash
ansible-playbook playbook-root.yml -i inventories/ovh-vps.yml --vault-id passphrase.sh --tags postfix
```
  * Change the SPF record
