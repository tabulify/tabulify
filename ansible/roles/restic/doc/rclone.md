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
