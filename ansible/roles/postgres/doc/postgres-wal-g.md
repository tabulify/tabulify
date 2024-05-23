# Wal-g

```bash
#/bin/bash
# start the backup
envdir /etc/wal-g.d/env /usr/local/bin/wal-g backup-push /usr/local/var/pg15/

# To list the backups
envdir /etc/wal-g.d/env /usr/local/bin/wal-g backup-list

# Delete the older than 5 days backup
envdir /etc/wal-g.d/env /usr/local/bin/wal-g delete retain Full 5 --confirm
```
