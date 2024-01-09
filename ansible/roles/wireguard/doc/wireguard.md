# Wireguard


[wg_tool](https://github.com/gene-git/wg_tool) — Tool to manage wireguard configs for server and users.

https://github.com/pirate/wireguard-docs/tree/master/example-internet-browsing-vpn

## Private IP

The private IP ranges defined by the [RFC 1918](https://tools.ietf.org/html/rfc1918) are the following:

10.0.0.0/8
172.16.0.0/12
192.168.0.0/16

https://www.procustodibus.com/blog/2022/09/wireguard-port-forward-from-internet/

## Utility
https://www.procustodibus.com/blog/2021/11/wireguard-nftables/#troubleshooting

Check the route
```
ping
```

Dump the packet
```
tcpdump -ni any 'tcp port 80 and host 192.168.200.22'
```


## Fly / Container

https://github.com/magJ/fly-wireguard-vpn-proxy
https://github.com/linuxserver/docker-wireguard

## Topology
Just good to know
  * https://www.procustodibus.com/blog/2020/10/wireguard-topologies/
  * https://www.procustodibus.com/blog/2021/07/wireguard-firewalld/
  * https://www.procustodibus.com/blog/2021/07/wireguard-firewalld/#troubleshooting

## Ref / Manpage

https://manpages.debian.org/unstable/wireguard-tools/wg-quick.8.en.html
