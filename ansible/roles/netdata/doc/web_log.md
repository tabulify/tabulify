# Web Log Debug


## About
How to diagnose any web_log problem

Web Log run with the [python collector](https://learn.netdata.cloud/docs/agent/collectors/python.d.plugin/)

## Steps

  * Go as netdata

```bash
sudo su -s /bin/bash netdata
```

  * Check that the plugin is running (ie go.d.plugin, python.d.plugin and )
```bash
systemctl status netdata
# or
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

### Go

```bash
cd /usr/libexec/netdata/plugins.d/
sudo -u netdata -s
./go.d.plugin -d -m web_log
```
```console
[ DEBUG ] main[main] main.go:115 plugin: name=go.d, version=v0.54.1
[ DEBUG ] main[main] main.go:117 current user: name=netdata, uid=996
[ INFO  ] main[main] main.go:121 env HTTP_PROXY '', HTTPS_PROXY '' (both upper and lower case are respected)
[ INFO  ] main[main] agent.go:140 instance is started
[ INFO  ] main[main] setup.go:43 loading config file
[ INFO  ] main[main] setup.go:51 looking for 'go.d.conf' in [/etc/netdata /usr/lib/netdata/conf.d]
[ INFO  ] main[main] setup.go:58 found '/usr/lib/netdata/conf.d/go.d.conf
[ INFO  ] main[main] setup.go:65 config successfully loaded
[ INFO  ] main[main] agent.go:144 using config: enabled 'true', default_run 'true', max_procs '0'
[ INFO  ] main[main] setup.go:70 loading modules
[ INFO  ] main[main] setup.go:89 enabled/registered modules: 1/79
[ INFO  ] main[main] setup.go:94 building discovery config
[ INFO  ] main[main] setup.go:141 looking for 'web_log.conf' in [/etc/netdata/go.d /usr/lib/netdata/conf.d/go.d]
[ INFO  ] main[main] setup.go:157 found '/etc/netdata/go.d/web_log.conf
[ INFO  ] main[main] setup.go:162 dummy/read/watch paths: 0/1/0
[ DEBUG ] web_log[datacadamia] init.go:128 created parser: ltsv: map[]
[ WARN  ] web_log[datacadamia] weblog.go:114 check failed: verify last line: verify: empty line (2a01:4f8:a0:5385::2 - - [31/Mar/2022:13:54:07 +0000] "GET /db                                    /hana/service?do=search&sf=1&q=2*%20@db:hadoop:hue HTTP/2.0" 200 33797 "-" "Mozilla/5.0 (compatible; MJ12bot/v1.4.8; http://mj12bot.com/)")
[ ERROR ] web_log[datacadamia] job.go:205 check failed
[ DEBUG ] web_log[datacadamia] reader.go:129 close log file: /var/log/nginx/datacadamia.com/https-access.log
```
https://learn.netdata.cloud/docs/data-collection/web-servers-and-web-proxies/web-server-log-files

### Old Python

  * Get the path of the python plugin and ask a debug trace
```bash
/usr/bin/python /usr/libexec/netdata/plugins.d/python.d.plugin web_log debug trace
```
