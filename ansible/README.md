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
```

## TODO

  * https://github.com/fail2ban/fail2ban
  * OVH Firewall
  
## Documentation

  * [Securisation](https://docs.ovh.com/fr/vps/conseils-securisation-vps/)