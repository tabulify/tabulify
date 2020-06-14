# Netdata Ansible Role Installation and Configuration


## About

An [Ansible](https://www.ansible.com) role to install/configure [Netdata](https://my-netdata.io/)

https://packagecloud.io/netdata/netdata/install#manual-rpm


## Usage

  * Install the role

````bash
ansible-galaxy install --roles-path . gerardnico.netdata
````

  * Copy the [defaults/main.yml](defaults/main/main.yml) into your vars.

  * Use it in your [playbook_example.yml](playbook_example.yml)

## Reference

Based on:

  * https://github.com/mrlesmithjr/ansible-netdata 
  * https://docs.netdata.cloud/packaging/installer/#install-netdata-on-linux-manually

## FYI

  * [Prometheus as Backend](https://docs.netdata.cloud/backends/prometheus/)
  * [Netdata, Prometheus, Grafana](https://docs.netdata.cloud/backends/prometheus/)
  * [Netdata with Prometheus](https://docs.netdata.cloud/backends/prometheus/)
