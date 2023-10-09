
## Deploy

```bash
fly deploy --local-only # with the local docker
```

## Status

```bash
fly status
```

## Machine

```bash
fly machine stop # Start machine
fly machine start
```

## Console

```bash
fly ssh console # Log into the machine
```

## Tunnel

```bash
fly wireguard list
fly wireguard remove
```

## Network
6PN (for IPv6 Private Networking) - Virtual Private Cloud (VPC)
Instances are configured:
  * with their DNS server pointing to `fdaa::3`
  * with an additional IPv6 address (6PN address), in `/etc/hosts` as `fly-local-6pn`
```
dig +short aaaa toweredge.internal @fdaa::3
```
https://fly.io/docs/reference/private-networking/
https://fly.io/blog/incoming-6pn-private-networks/

### /etc/host

```
# Address in the 6PN private network for this app
fdaa:3:1227:a7b:144:cf86:6a7b:2 fly-local-6pn

# Private address for this instance
172.19.132.34   6e82d92ef7d768

# Address used for global traffic routing
172.19.132.35   fly-global-services

# Private address for this instance
2604:1380:4541:2e02:0:cf86:6a7b:1       6e82d92ef7d768
```

### Binding / Query / Curl

After a [](#console)

* Public
```bash
curl http://0.0.0.0:8084
curl http://fly-local-6pn:8084
curl http://toweredge.internal:8084/status
```
* with a [tunnel](#tunnel)
```bash
curl http://toweredge.internal:8084/status
```
```bash
nslookup toweredge.internal
Server:  UnKnown
Address:  fdaa:3:1227::3

Name:    toweredge.internal
Address:  fdaa:3:1227:a7b:144:cf86:6a7b:2
```
https://fly.io/docs/getting-started/app-services/
