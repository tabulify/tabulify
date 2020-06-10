# Postfix installation and configuration

## About

This role install the mail transfer agent `Postfix` in order to be able to send email.


## Conf

The two most important files are (root only) accessible at:
 
  * /etc/postfix/main.cf 
  * /etc/postfix/master.cf

You specify a configuration parameter as: 

```ini
parameter = value
```
and you use it by putting a "$" character in front of its name: 

```ini
other_parameter = $parameter
```

See the conf
```bash
postconf 
```

To find out what default settings are overruled by the main.cf.
```bash
postconf -n 
``` 

## Service

The service runs under the user `postfix` (created at install ) configured at `/etc/postfix/main.cf`

```ini
mail_owner = postfix
```

## Security

Postfix forwards mail only:
  * from clients in trusted networks, 
  * from clients that have authenticated with SASL, 
  * or to domains that are configured as authorized relay destinations.
 
By default, Postfix will forward mail:
  * from strangers (clients outside authorized networks) to authorized remote destinations (`relay_domains`) only 
 
http://www.postfix.org/SMTPD_ACCESS_README.html


## Test

Send a mail with mail (mail is installed with the mailx package)

```bash
echo "This is the body of the email" | mail -s "This is the subject line" user@example.com
```


https://www.mail-tester.com/


## Log

Postfix daemon processes run in the background, and log problems and normal activity to the syslog daemon.

```bash 
tail -f /var/log/maillog
# or
egrep '(warning|error|fatal|panic):' /var/log/maillog | more
```

Log Conf is done via `/etc/rsyslog.conf`.

More:
 
  * http://www.postfix.org/DEBUG_README.html
  * http://www.postfix.org/BASIC_CONFIGURATION_README.html#syslog_howto

## Doc

  * Most of the configuration comes from: http://www.postfix.org/BASIC_CONFIGURATION_README.html 
  * https://wiki.centos.org/HowTos/postfix


