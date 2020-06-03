# PHP Diagnostic


## Basic File are accessible ?

Test:

  * [VERSION](https://gerardnico.com/VERSION)
  * [phpinfo](https://gerardnico.com/phpinfo-nico.php)



## Nginx ?

If the VERSION file test is not running, check [nginx](../../nginx/diagnostic.md)

If you got a error status (500), you can see it
```bash
tail -f /var/log/nginx/gerardnico.com/https-access.log
tail -f /var/log/nginx/gerardnico.com/https-error.log
```
No error and the bad status is in the access log, it's not nginx.

## Php-fpm is running ?

If the `phpinfo` file test is not running, check php-fpm

```bash
systemctl status php-fpm
```


## Check the log file

If the `phpinfo` file is running, check the log. 

The log can be seen in the `error_log`
property of [phpinfo](https://gerardnico.com/phpinfo-nico.php)
This is not the log defined in `php.ini` but in the [fpm spool conf](../templates/php-fpm-spool-www.conf.ini) at the
property `php_admin_value[error_log]`
 
Example:

```bash
tail -f /var/log/php-fpm/www-error.log
```


## Display errors on the page

  * at the command line

```bash
php -d error_reporting=-1 -d display_errors=On ./bin/plugin.php  extension
```

  * Otherwise, to display the log on the page, you can change the errors property of `php.ini` in the [main.yml](../defaults/main.yml)
  and run the playbook

Example: 
```ini
php_error_reporting: 'E_ALL' 
php_display_error: 'On'
```
   
    
