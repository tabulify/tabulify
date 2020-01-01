# Ansible Infra Configuration


## Run

  * Copy the file [passphrase-dist.sh](passphrase-dist.sh) to `passphrase.sh`
  * Change the password 
  * Run it the first time with the following extra vars:
     * `ssh_actual_port=22` - after the first run, the new default ssh port will be then changed to `2222`
     * `ansible_user=root` - after the first run, a user will be created and root connection will be refused
     * `ansible_password=root` - the root password to be able to connect
```bash
ansible-playbook playbook-root.yml -i inventories/ovh-vps.yml --vault-id passphrase.sh --extra-vars "ssh_actual_port=22 ansible_user=root ansible_password=changeme"
```
  * Subsequent run does not need this extra vargs
```bash
ansible-playbook playbook-root.yml -i inventories/ovh-vps.yml --vault-id passphrase.sh
# or with tags
ansible-playbook playbook-root.yml -i inventories/ovh-vps.yml --vault-id passphrase.sh --tags netdata
```

## Application available

  * http://nexus.bytle.net
  * http://netdata.bytle.net
  

## TODO

  * [OVH Firewall](https://docs.ovh.com/fr/dedicated/firewall-network/)
  * get public key authentication working before disabling PasswordAuthentication in sshd_config.

## Ter info

Encryption
```bash
ansible-vault encrypt_string --vault-id passphrase.sh 'the_password_to_encrypt'
```  
## Documentation

  * [Securisation](https://docs.ovh.com/fr/vps/conseils-securisation-vps/)
