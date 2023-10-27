# Openjdk

## About

An Ansible role to install `openjdk`


## Run

```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags openjdk
```

# Usage

```yaml
- name: Playbook
  roles:
    - name: Install openjdk
      role: openjdk
      openjdk_version: 11.0.2
```
where:
  * `openjdk_version` is based on available yum packages values include: 1.6.0, 1.7.0, 1.8.0, 11, ...

# Documentation / Reference
  * https://openjdk.org/install/
  * Inspired by [Asymmetrik openjdk](https://github.com/Asymmetrik/ansible-roles/tree/master/openjdk)
