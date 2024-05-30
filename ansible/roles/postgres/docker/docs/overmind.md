# Overmmind

## Process manager

As it works with Tmux (ie tty), we can't get the log of a process easily.

* https://fly.io/docs/app-guides/multiple-processes/#use-a-procfile-manager
* https://github.com/DarthSim/overmind

## Procfile

ie `Procfile`

```Procfile
postgres: postgres-entrypoint.sh postgres -c config_file=/etc/postgresql/postgresql.conf
postgres_exporter: postgres-exporter-entrypoint.sh
sql_exporter: sql-exporter-entrypoint.sh
```

## Docker installation

```Dockerfile
RUN curl -L "https://github.com/DarthSim/overmind/releases/download/v${OVERMIND_VERSION}/overmind-v${OVERMIND_VERSION}-linux-amd64.gz" -o overmind.gz && \
    gunzip overmind.gz && \
    chmod +x overmind && \
    mv overmind /usr/local/bin/

# Overmind dep
RUN apt-get install --no-install-recommends -y tmux

# Procfile should be located in the working directory (or -f path/to/your/Procfile)
ADD Procfile .
ADD docker/entrypoint/overmind-entrypoint.sh /usr/local/bin/
# wrapper around overmind to allow customization
RUN chmod +x /usr/local/bin/overmind-entrypoint.sh
CMD ["overmind-entrypoint.sh"]
```

## Entrypoint

```bash
#!/bin/bash

# Env
# .bashrc to bring the connection environments
. /root/.bashrc

# We start as daemon to get all the logs in the same proc (ie proc 1)
command="${OVERMIND_ENV:-OVERMIND_CAN_DIE=sql_exporter,postgres_exporter OVERMIND_SHOW_TIMESTAMPS=1} overmind start"
printf "\nStarting the main process. Command:\n%s\n" "$command"
eval "$command"
```
