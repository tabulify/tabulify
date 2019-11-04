@echo off

ansible-playbook playbook-root.yml -i inventories/ovh-vps.yml --vault-id passphrase.sh
