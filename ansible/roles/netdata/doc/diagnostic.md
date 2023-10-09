# Netdata


## Services

```bash
systemctl status netdata
systemctl stop netdata
systemctl start netdata
```

## Daemon
See the daemon processes (the go daemon was not shipped, python is)

```bash
ps faxu | grep "[n]etdata"
```
```
netdata  11761  1.3  2.3 367204 87880 ?        Ssl  18:19   0:19 /usr/sbin/netdata -P /var/run/netdata/netdata.pid -D
netdata  11765  0.0  0.0  74640  2256 ?        Sl   18:19   0:00  \_ /usr/sbin/netdata --special-spawn-server
netdata  11865  0.1  0.0   9700  1540 ?        S    18:19   0:01  \_ /usr/bin/bash /usr/libexec/netdata/plugins.d/tc-qos-helper.sh 1
netdata  11875  1.5  0.1  60568  3972 ?        S    18:19   0:22  \_ /usr/libexec/netdata/plugins.d/apps.plugin 1
netdata  11888  0.2  0.5 143152 21992 ?        Sl   18:19   0:04  \_ /usr/bin/python /usr/libexec/netdata/plugins.d/python.d.plugin 1
```


## Testing File Permission

Switch to the netdata user to see if it has the permissions on the log

```bash
sudo -u netdata -s
```


```bash
tail -f /var/log/netdata/error.log
```

## Web Log

[web_log](web_log.md)
