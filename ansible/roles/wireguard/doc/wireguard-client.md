# Client Configuration



wg0.conf
```ini
[Interface]
Address = 192.168.71.1/24,fdc9:3c6b:21c7:e6bd::2/64
PrivateKey = <client_one_private_key>
ListenPort = 51821

[Peer]
PublicKey = <server_public_key>
PresharedKey = <client_one_psk>
Endpoint = <ServerAddress>:51820
# Route all IPv4 and IPv6 the traffic on the client computer over the WireGuard interface:
AllowedIPs = 0.0.0.0/0,::/0
```
