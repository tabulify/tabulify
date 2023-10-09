# Server Common and Securization

## About

This role will secure the server as describe
in this [doc](https://docs.ovh.com/fr/vps/conseils-securisation-vps/)

## Run

See [](../../README.md)

## Configuration

The `hostname` should be set in the [inventory file](../../inventories) with the `FQDN` format.

## TODO

Not yet automatic: IPV6 -
  * [Step 1 - IPV6](https://docs.ovh.com/us/en/vps/configuring-ipv6/#persistent-application-on-redhat-and-its-derivatives-centos-clearos_1)
  * [Step2 - Disable Cloud-init](https://docs.ovh.com/us/en/vps/configuring-ipv6/#step-4-disable-cloud-init-network-management)


## No sudo

Note that without sudo, you can still execute administrative task with pkexec

Example for a bad:
```bash
pkexec rm /etc/sudoers.d/tower
```
