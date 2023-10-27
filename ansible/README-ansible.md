# Ansible Infra Configuration


## Next

The current CentOs is EOL on

Next is a server on docker (to be able to install the needed dependency such as a sqlite version) ?
Ovh install it on Debian.

## Run

  * Copy the file [passphrase-dist.sh](passphrase-dist.sh) to `passphrase.sh`
  * Change the password
  * Copy the [inventory](inventories/beau.yml) file to create new one in case of migration
  * Change the following variable:
    * `ansible_host`
    * `hostname`
  * Run it the first time with:
    * a inventory file
    * and the following extra vars to connect the first time and overwrite [ssh](group_vars/ovh/ssh.yml):
       * `ansible_user=root` - after the first run, a user will be created and root connection will be refused
       * `ansible_password=changeme` - the root password to be able to connect
```bash
ansible-playbook playbook-root.yml -i inventories/changeme.yml --vault-id passphrase.sh --extra-vars "ansible_user=root ansible_password=changeme"
```
  * Subsequent run does not need this extra vargs, it will use the credentials in [ssh](group_vars/ovh/ssh.yml)
```bash
ansible-playbook playbook-root.yml -i inventories/changeme.yml --vault-id passphrase.sh
# or with tags
ansible-playbook playbook-root.yml -i inventories/changeme.yml --vault-id passphrase.sh --tags os
```

## Ad hoc

  * Getting all facts

```bash
ansible -i inventories/beau.yml --vault-id passphrase.sh  -m setup all
```

  * Getting a variable value

```bash
ansible -i inventories/beau.yml --vault-id passphrase.sh  -m debug -a var=ansible_host all
```

  * Getting an expression

```bash
ansible -i inventories/beau.yml --vault-id passphrase.sh -m debug -a "msg={{ ansible_host.split('.', 1)[0] }}" all
```

## Application available

  * https://nexus.bytle.net (Port 8082)
  * https://netdata.bytle.net (Port 19999)
  * https://api.gerardnico.com (Port 8083)


## TODO

  * [OVH Firewall](https://docs.ovh.com/fr/dedicated/firewall-network/)
  * get public key authentication working before disabling PasswordAuthentication in sshd_config.

## Password Encryption

Encryption
```dos
ansible-bash
```
then
```bash
ansible-vault encrypt_string --vault-id passphrase.sh 'the_password_to_encrypt'
# for a file
cat cert.pem | ansible-vault encrypt_string --vault-id passphrase.sh
```


Decryption
```bash
echo '$ANSIBLE_VAULT;...<ansible vault string>' | tr -d ' ' | ansible-vault decrypt --vault-id passphrase.sh && echo
```

## Documentation

  * [Securisation](https://docs.ovh.com/fr/vps/conseils-securisation-vps/)
