# Openjdk

## About

An Ansible role to install `openjdk`


## Prerequisites

A RedHat OS family (yum). See [meta](meta/main.yml)


# Usage

```yaml
- name: Playbook
  roles:
    - name: Install openjdk
      role: openjdk
      openjdk_version: 1.8.0
```
where:
  * `openjdk_version` is based on available yum packages values include: 1.6.0, 1.7.0, 1.8.0, 11, latest

# Documentation / Reference

  * Inspired by [Asymmetrik openjdk](https://github.com/Asymmetrik/ansible-roles/tree/master/openjdk)


