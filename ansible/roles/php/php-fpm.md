# Php Fpm

## About

The `php-fpm` service is used because of nginx that does not have php support natively.

```bash
sudo systemctl status php-fpm
```

Php-fpm is a fastcgi process manager for php that is totally separate from the webserver. The webserver communicates with 
fpm through a socket and passes the name of the script to execute. 
So fpm can run with any web server that is fastcgi compatible.

## Conf

The conf is at:
   
   * /etc/php-fpm.conf 

that includes the pool configuration file 

  * /etc/php-fpm.d/*.conf


## Pool

A pool is a process that run under a user.

The default configuration file called www.conf 
which can be copied to create more pool. 

Each file must end with .conf to be recognised 
as a pool configuration file by php fpm.

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
           ├─18508 php-fpm: pool poolName
           ├─18509 php-fpm: pool poolName
           ├─18510 php-fpm: pool poolName
           ├─18511 php-fpm: pool poolName
           └─18512 php-fpm: pool poolName
```

## Log
  * For php-fpm: /var/log/php-fpm/errors.log
  * For a website (a pool), in the conf file `php_admin_value[error_log] = /var/log/php-fpm/gnico-error.log`
