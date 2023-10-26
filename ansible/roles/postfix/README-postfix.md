# Postfix installation and configuration

## About

This role install the mail transfer agent `Postfix` in order to be able to send email.

## Run

```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags postfix -v
```

Test Dkim Value
```bash
ansible-playbook playbook-root.yml -i inventories/beau.yml --vault-id passphrase.sh --tags dkim-test-key -v
```

## Installation on a new server

  * In the [public key](public_dns_key.txt), change the server name
  * Add it into the DNS zone.

## How to

### Add a new mail alias

See [Add new mail alias](mail.md)

### Add a new domain to sign incoming email

  * To add a new domain for signature
  * Add the domain in `dkim_domains`
  * Create the DKIM DNS key `dkim_selector`.domain._domainkey with the public key

Example with the actual value for the selector as default.

```
eraldy._domainkey        IN TXT    ( "v=DKIM1; k=rsa; p=thePublicKey" )
```


## TODO

  * certificat is now linked to the machine name, it should be also linked to an alias such as smtp.bytle.net
  * mail for root should always be aliased to a real user [Ref](http://www.postfix.org/postconf.5.html#mailbox_command)

https://linuxlasse.net/linux/howtos/Blacklist_and_Whitelist_with_Postfix
https://ethitter.com/2016/03/blocking-sender-ips-in-postfix/
https://serverfault.com/questions/481477/in-postfix-how-to-enforce-tls-auth-over-587-while-leaving-tls-optional-for-25

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

To lookup a conf:
```bash
postconf -h parameter
# example
postconf -h mydomain
```

## Service

The service runs under the user `postfix` (created at install ) configured at `/etc/postfix/main.cf`

```ini
mail_owner = postfix
```

## Security

By default, Postfix relays mail
  * from "trusted" clients (IP address matches $mynetworks) to any destination,
  * from "untrusted" clients to destinations that match $relay_domains or subdomains thereof, except addresses with sender-specified routing.

The [SMTP server](http://www.postfix.org/smtpd.8.html) rejects mail for unknown recipients. See [KNOWN VERSUS UNKNOWN RECIPIENT CONTROLS](http://www.postfix.org/smtpd.8.html)

The default relay_domains value is $mydestination.

In addition to the above, the Postfix SMTP server by default accepts mail that Postfix is final destination for:
  * destinations that match $inet_interfaces or $proxy_interfaces,
  * destinations that match $mydestination
  * destinations that match $virtual_alias_domains,
  * destinations that match $virtual_mailbox_domains.
These destinations do not need to be listed in $relay_domains.

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
