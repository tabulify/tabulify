# Certbot

## About

This role will install certbot of let's encrypt in order to get certificate.

Certificate are used:

  * in website ([nginx](../nginx/README.md))
  * for the server (smtp). See [Postfix](../postfix/README.md)

## Usage

  * Add the domain in the [domains](./defaults/main/domains.yml) file
  * Run this role

```bash
ansible-playbook playbook-root.yml -i inventories/ovh-vps.yml --vault-id passphrase.sh --tags certbot
``` 


## TODO May be 

Ansible has also a module called [acme_certificate](https://docs.ansible.com/ansible/2.9/modules/acme_certificate_module.html#acme-certificate-module)

More see this [blog](https://www.digitalocean.com/community/tutorials/how-to-acquire-a-let-s-encrypt-certificate-using-ansible-on-ubuntu-18-04)
