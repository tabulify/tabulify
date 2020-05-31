
## Conf file

They can be found at: 
```bash
ls -A1 /etc/nginx/conf.d/*.conf
```
```text
/etc/nginx/conf.d/gnico.conf
/etc/nginx/conf.d/gogs.conf
/etc/nginx/conf.d/netdata.conf
/etc/nginx/conf.d/nexus.conf
/etc/nginx/conf.d/prometheus.conf
```
## Read a certification

```
openssl x509 -in cert.pem -text -noout
```

## By Service 

### Gnico

  * If the status is 502, the service is done ? 

```bash
systemctl status gnico
# then
systemctl start gnico
```

  * If the endpoint is bad = 404

Test a good one: [https://api.gerardnico.com/ip](https://api.gerardnico.com/ip)



