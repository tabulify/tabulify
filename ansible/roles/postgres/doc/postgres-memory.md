# Postgres Memory


## Internal
[Memory Parameters documentation](https://www.postgresql.org/docs/current/runtime-config-resource.html#RUNTIME-CONFIG-RESOURCE-MEMORY)

See parameters in [postgres.conf](../templates/postgresql.conf.ini)

```bash
ActualMaxRAM = shared_buffers + (temp_buffers + work_mem) * max_connections
# default
ActualMaxRAM = 128Mb + (8MB + 4Mb) * 100 = 1326Mb
# actual
ActualMaxRAM = 128Mb + (1MB + 5Mb) * 50 = 378Mb
```

* shared_buffers: 40% of the memory
* max_connections: maximum number of parallel connections
* temp_buffers (used for temporary tables (dropped at the end of a session), setting temp_buffers to pretty low (default is 8 MB) will allow setting work_mem a bit higher.)
* work_mem: working memory


https://stackoverflow.com/questions/28844170/how-to-limit-the-memory-that-is-available-for-postgresql-server


## OS resources

https://www.postgresql.org/docs/15/kernel-resources.html#SYSVIPC
