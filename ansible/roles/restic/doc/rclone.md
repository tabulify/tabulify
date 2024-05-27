# Rclone


## Conf
```
rclone config file
/root/.config/rclone/rclone.conf
```

## Op

```
rclone delete iDriveBackup:/backup/hallWorld.ini
rclone lsf iDriveBackup:
rclone lsf iDriveBackup:/backup
rclone copy hallWorld.ini iDriveBackup:/backup
```

### Delete the content of the bucket

```
rclone purge idrive-restic:/restic/*
```

## Docker

```Dockerfile
######################################
# Rclone
# https://rclone.org/install/#linux
######################################
# https://rclone.org/downloads/
ARG RCLONE_VERSION=1.66.0
RUN curl -L https://downloads.rclone.org/v${RCLONE_VERSION}/rclone-v${RCLONE_VERSION}-linux-amd64.zip -o rclone.zip && \
    unzip rclone.zip && \
    rm rclone.zip && \
    cd rclone-*-linux-amd64 && \
    chmod +x rclone && \
    mv rclone /usr/local/bin/
```
