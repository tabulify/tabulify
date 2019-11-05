# Openjdk

## About

An Ansible role to install `openjdk` via yum.


## Prerequisites

Assumes a Unix/Linux OS, and has only been tested on the RedHat OS family

See [meta](meta/main.yml)

## Variables

### Version  

Based on available yum packages values include: 1.6.0, 1.7.0, 1.8.0, 11, latest
```yaml
openjdk_version: 1.8.0
```

# Usage

```yaml
- name: Playbook
  roles:
    - name: Install openjdk
      role: openjdk
      openjdk_version: 1.8.0
```

# Documentation / Reference

  * Inspired by [Asymmetrik openjdk](https://github.com/Asymmetrik/ansible-roles/tree/master/openjdk)


