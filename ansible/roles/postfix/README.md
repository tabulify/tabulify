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


## Service

The service runs under the user `postfix` (created at install ) configured at `/etc/postfix/main.cf`

```ini
mail_owner = postfix
```

## Test

https://www.mail-tester.com/

## Log

Postfix daemon processes run in the background, and log problems and normal activity to the syslog daemon.

IMPORTANT: many syslogd implementations will not create files. You must create files before (re)starting syslogd.

/var/log/mail.log

  * http://www.postfix.org/DEBUG_README.html
  * http://www.postfix.org/BASIC_CONFIGURATION_README.html#syslog_howto

## Doc

  * Most of the configuration comes from: http://www.postfix.org/BASIC_CONFIGURATION_README.html 


