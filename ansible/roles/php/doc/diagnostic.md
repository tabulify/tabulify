# PHP Diagnostic



## Check the log file

Check the log. The log can be seen in the `error_log`
property of [phpinfo](https://gerardnico.com/phpinfo-nico.php)
This is not the log defined in `php.ini` but in the [fpm spool conf](../templates/php-fpm-spool-www.conf.ini) at the
property `php_admin_value[error_log]`
 
Example:

```bash
tail -f /var/log/php-fpm/www-error.log
```


## Display errors on the page

To display the log on the page, you can change the errors property of `php.ini`.

Example: 
```ini
php_error_reporting: 'E_ALL' 
php_display_error: 'On'
```

You can change them by running this role and changing this value that are set in the [main.yml](../defaults/main.yml)
file.
   
    
