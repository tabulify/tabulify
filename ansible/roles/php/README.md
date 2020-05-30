# Installation of php7.2


## Inspiration

See faq

https://blog.remirepo.net/pages/English-FAQ
https://rpms.remirepo.net/wizard/

  * `php-*` packages are the default one (ie provide the php.exe)
  * `phpxx-php-*` such as `php74-php-gd` provides another php installation

Already installed version 5.4

[root@vps748761 php-7.2.31]# php --version
PHP 5.4.16 (cli) (built: Apr  1 2020 04:07:17)
Copyright (c) 1997-2013 The PHP Group
Zend Engine v2.4.0, Copyright (c) 1998-2013 Zend Technologies

After

[root@vps748761 php-7.2.31]# php --version
PHP 7.4.6 (cli) (built: May 12 2020 08:09:15) ( NTS )
Copyright (c) The PHP Group
Zend Engine v3.4.0, Copyright (c) Zend Technologies


[root@vps748761 php-7.2.31]# php --modules
[PHP Modules]
bz2
calendar
Core
ctype
curl
date
exif
fileinfo
filter
ftp
gettext
hash
iconv
json
libxml
openssl
pcntl
pcre
Phar
readline
Reflection
session
sockets
SPL
standard
tokenizer
zlib

[root@vps748761 php-7.2.31]#  php --ini
Configuration File (php.ini) Path: /etc
Loaded Configuration File:         /etc/php.ini
Scan for additional .ini files in: /etc/php.d
Additional .ini files parsed:      /etc/php.d/20-bz2.ini,
/etc/php.d/20-calendar.ini,
/etc/php.d/20-ctype.ini,
/etc/php.d/20-curl.ini,
/etc/php.d/20-exif.ini,
/etc/php.d/20-fileinfo.ini,
/etc/php.d/20-ftp.ini,
/etc/php.d/20-gd.ini,
/etc/php.d/20-gettext.ini,
/etc/php.d/20-iconv.ini,
/etc/php.d/20-json.ini,
/etc/php.d/20-phar.ini,
/etc/php.d/20-sockets.ini,
/etc/php.d/20-tokenizer.ini


`php-fpm` client is used because of nginx that does not have php support natively

```bash
sudo systemctl status php-fpm
```

conf at /etc/php-fpm.conf that includes /etc/php-fpm.d/*.conf

Php-fpm is a fastcgi process manager for php that is totally separate from the webserver. The webserver communicates with fpm through a socket and passes the name of the script to execute. 
So fpm can run with any web server that is fastcgi compatible.

## Pool

A file called www.conf already exists which can be copied to create more pool configuration files. 
Each file must end with .conf to be recognised as a pool configuration file by php fpm.

```apache
user = mysite_user
group = mysite_user
```

The pool name can be seen in the status of FPM
```bash
systemctl status php-fpm 
```
```text
 php-fpm.service - The PHP FastCGI Process Manager
   Loaded: loaded (/usr/lib/systemd/system/php-fpm.service; enabled; vendor preset: disabled)
   Active: active (running) since Sat 2020-05-30 23:57:17 CEST; 8s ago
  Process: 18505 ExecReload=/bin/kill -USR2 $MAINPID (code=exited, status=0/SUCCESS)
 Main PID: 18473 (php-fpm)
   Status: "Ready to handle connections"
   CGroup: /system.slice/php-fpm.service
           ├─18473 php-fpm: master process (/etc/php-fpm.conf)
           ├─18508 php-fpm: pool gnico
           ├─18509 php-fpm: pool gnico
           ├─18510 php-fpm: pool gnico
           ├─18511 php-fpm: pool gnico
           └─18512 php-fpm: pool gnico
```

## Log
  * For php-fpm: /var/log/php-fpm/errors.log
  * For a website (a pool), in the conf file `php_admin_value[error_log] = /var/log/php-fpm/gnico-error.log`

## Ref

https://gist.github.com/fyrebase/62262b1ff33a6aaf5a54

