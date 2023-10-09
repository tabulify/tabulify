# IDrive

## About
We choose IDrive because:
* it was free until 10Gb.
* it's 20 euro by year otherwise

## Tools

no_check_bucket = true


https://www.idrive.com/object-storage-e2/s3fs

rclone move /root/helloWorld.txt iDriveBackup:backup

https://www.idrive.com/object-storage-e2/winscp-guide


https://www.idrive.com/object-storage-e2/cloudflare-guide


## Encryption

* Default encryption and private encryption, use the 256-bit AES encryption to encrypt your data.
* Default encryption uses a system generated key, whereas for private encryption, a user-defined key is used.

## Rclone
https://www.idrive.com/object-storage-e2/rclone

```ini
[iDriveBackup]
type = s3
provider = IDrive
access_key_id = xxxx
secret_access_key = xxxx
endpoint = h0k0.ca.idrivee2-22.com
no_check_bucket = true
server_side_encryption = aws:kms
```

https://www.airlivedrive.com/en/
