# Netdata


## About

An [Ansible](https://www.ansible.com) role to install/configure [Netdata](https://my-netdata.io/)

Based on [rpm](https://packagecloud.io/netdata/netdata/install#manual-rpm)

## Netdata business model

Netdata is an agent with  minimal functionality such as alerting, UI.
If you want to create dashboard you need a Netdata cloud account (cost is 3 euro by node)

## GUI

https://beau.bytle.net/netdata/

## Reference

Based on:
  * https://docs.netdata.cloud/packaging/installer/#install-netdata-on-linux-manually

They have also written in the meantime a playbook. Check it out at
[Ansible Netdata](https://learn.netdata.cloud/guides/deploy/ansible)

## Conf

  * To get a version of the conf file `netdata.conf` with all possible properties commented at [Actual Conf](https://netdata.bytle.net/netdata.conf)
  * To edit or create a conf file, you can use the [edit-conf utility](https://learn.netdata.cloud/docs/configure/nodes#use-edit-config-to-edit-configuration-files)

!!! location is not the same as in the doc !!!

```bash
/usr/libexec/netdata/edit-config
```


## Diagnostic

See [diagnostic](doc/diagnostic.md)

## FYI

  * [Prometheus as Backend](https://docs.netdata.cloud/backends/prometheus/)
  * [Netdata, Prometheus, Grafana](https://docs.netdata.cloud/backends/prometheus/)
  * [Netdata with Prometheus](https://docs.netdata.cloud/backends/prometheus/)

## Run

```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags netdata
```

## Documentation

See also the [monitoring documentation](../nginx/doc/monitoring.md)
