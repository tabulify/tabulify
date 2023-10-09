# Web Log Debug


## About
How to diagnose any web_log problem

Web Log run with the [python collector](https://learn.netdata.cloud/docs/agent/collectors/python.d.plugin/)

## Steps

  * Go as netdata

```bash
sudo su -s /bin/bash netdata
```

  * Check which python.d.plugin is running
```bash
ps fax
```
```
25471 ?        Ssl    0:04 /usr/sbin/netdata -P /var/run/netdata/netdata.pid -D
25476 ?        Sl     0:00  \_ /usr/sbin/netdata --special-spawn-server
25587 ?        S      0:00  \_ bash /usr/libexec/netdata/plugins.d/tc-qos-helper.sh 1
25589 ?        S      0:04  \_ /usr/libexec/netdata/plugins.d/apps.plugin 1
25592 ?        Z      0:00  \_ [ebpf.plugin] <defunct>
25594 ?        Sl     0:00  \_ /usr/libexec/netdata/plugins.d/go.d.plugin 1
25599 ?        Sl     0:01  \_ /usr/bin/python /usr/libexec/netdata/plugins.d/python.d.plugin 1
```

  * Get the path of the python plugin and ask a debug trace
```bash
/usr/bin/python /usr/libexec/netdata/plugins.d/python.d.plugin web_log debug trace
```

