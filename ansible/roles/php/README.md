# Installation of php

## About

This role install php version 7.4 on Centos via an update of
the default php via the the remi repository.

Because the web server is Nginx, [php-fpm](php-fpm.md) was installed.
It's a service that makes a bridge between php and nginx.


## Documentation / Reference

  * The [remi wizard](whttps://rpms.remirepo.net/wizard/) gives the installation instructions
If you want to know more, read the [FAQ](https://blog.remirepo.net/pages/English-FAQ)

## Package

This is just general information. This role upgrade the installed php and therefore used
only the package `php-*`

There is two kind of php packages:

  * `php-*` packages are the default one of the system
  * `phpxx-php-*` such as `php74-php-gd` provides another php installation

## Version

### Before installation 

On Centos, `php` is already installed version 5.4

```bash
php --version
```
```text
PHP 5.4.16 (cli) (built: Apr  1 2020 04:07:17)
Copyright (c) 1997-2013 The PHP Group
Zend Engine v2.4.0, Copyright (c) 1998-2013 Zend Technologies
```

### After installation

```bash
php --version
```
````text
PHP 7.4.6 (cli) (built: May 12 2020 08:09:15) ( NTS )
Copyright (c) The PHP Group
Zend Engine v3.4.0, Copyright (c) Zend Technologies
````

## Admin Snippet

### Php modules

To see all installed modules

```bash
php --modules
```
```text
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
```

### Php.ini
```bash
php --ini
```
```text
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
```
