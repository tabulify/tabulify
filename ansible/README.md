# Ansible Infra Configuration


## Run

  * Copy the file [passphrase-dist.sh](passphrase-dist.sh) to `passphrase.sh`
  * Change the password 
  * Run it the first time with the extra vars `ssh_actual_port`. It will then change the port to `2222`
```bash
ansible-playbook playbook-root.yml -i inventories/ovh-vps.yml --vault-id passphrase.sh --extra-vars "ssh_actual_port=22"
```
  * Subsequent run does not need this extra vargs
```bash
ansible-playbook playbook-root.yml -i inventories/ovh-vps.yml --vault-id passphrase.sh
```

## Documentation

  * [Securisation](https://docs.ovh.com/fr/vps/conseils-securisation-vps/)