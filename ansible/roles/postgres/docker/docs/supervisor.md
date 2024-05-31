http://supervisord.org/running.html#running-supervisorctl

```bash
supervisorctl status all
supervisorctl stop all
supervisorctl tail [-f] <name> [stdout|stderr] (default stdout)
```

```bash
supervisorctl tail -f sql_exporter
supervisorctl start sql_exporter
```
