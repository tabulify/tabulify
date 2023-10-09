# Diagnostic page


## Web site up ?

http://sitemeer.com/#https://datacadamia.com

## Log HTTP Status

  * Check Live 4xx errors with:
```bash
tail -f /var/log/nginx/datacadamia.com/https-access.log | grep --line-buffered -P '" 4\d\d '
```
  * Parse
```bash
cat /var/log/nginx/datacadamia.com/https-access.log | grep -P '" 4\d\d '
```

Example of `444` (Nginx status to close a connection) due to a bot:
```
162.158.167.155 - - [17/Aug/2021:09:00:47 +0000] "GET /wp-login.php HTTP/2.0" 444 0 "-" "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko"
```

## Error 500

Go the FPM logging

   /var/log/php-fpm

```bash
tail -f  /var/log/php-fpm/error.log
```

## Port

  * Open

```
nmap -Pn -p T:80,443 vps-8f619f51.vps.ovh.ca
```
```text
PORT    STATE  SERVICE
80/tcp  open   http
443/tcp closed https
```

  * Listening

```text
netstat -tulpn|grep 80
```

## Go around Cloudflare


To test a DNS change, you can use the map function of the chrome browser.

```bash
chrome.exe --host-resolver-rules="MAP datacadamia.com 212.186.33.26"
```
Example:
```dos
cd /D "C:\Program Files (x86)\Google\Chrome\Application\"
chrome.exe --host-resolver-rules="MAP bytle.net  192.99.55.226, MAP gerardnico.com 192.99.55.226, MAP datacadamia.com 192.99.55.226"
```

## Log file

They are by default at:

  * /var/log/nginx/
  * /var/log/nginx/{{ web site }}

/var/log/nginx/datacadamia.com

Example: Log in as root and check the log
```bash
tail -f  /var/log/nginx/gerardnico.com/https-error.log
tail -f  /var/log/nginx/gerardnico.com/https-error.log
```

## Conf file

They can be found at:
```bash
ls -A1 /etc/nginx/conf.d/*
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


## Module

The build module are located

```
/usr/share/nginx/modules
```

verify with

```
nginx -V 2>&1 | grep 'configure arguments' | sed 's/--/\n--/g' | grep prefix | cut --delimiter '=' --fields=2- | tr -d ' '
```


