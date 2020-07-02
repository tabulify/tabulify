# Diagnostic page 

## Go around Cloudflare


To test a DNS change, you can use the map function of the chrome browser.

```bash
chrome.exe --host-resolver-rules="MAP gerardnico.com 212.186.33.26"
```
Example:
```dos
cd /D "C:\Program Files (x86)\Google\Chrome\Application\"
chrome.exe --host-resolver-rules="MAP bytle.net  164.132.99.202, MAP gerardnico.com 164.132.99.202, MAP datacadamia.com 164.132.99.202"
```

## Log file

They are by default at:

  * /var/log/nginx/
  * /var/log/nginx/{{ web site }}

/var/log/nginx/gerardnico.com

Example: Log in as root and check the log
```bash
tail -f  /var/log/nginx/gerardnico.com/https-error.log
tail -f  /var/log/nginx/gerardnico.com/https-error.log
```

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



