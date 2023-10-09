# Anible Gogs Role

## About

A ansible role that install [Gogs](https://github.com/gogs/gogs) - A self-hosted Git service

## Installation

We explain first the [official installation](#official), then this [Ansible role installation](#role). 

### Official 

The [official installation](https://gogs.io/docs/installation) is:
  * download the binary
  * start gogs
```bash
./gogs web -port 3001
```
  * go to the address where you are redirected to the `/install` page, 
  where you can configure an administrator account and other default options.

### Role 
How to install Gogs with this role.

The installation is closed. The [configuration file](templates/app.ini) does not allow to see anything without to be logged in.

  * You need to give a mandatory variable: `gogs_mandatory_secret_key`. This is a secret used to cipher all sensitive information such as cookie password
  * You can overwrite the [default variable](defaults/main.yml) to your need
  * Run the role 
  * Create an admin
```bash
./gogs admin create-user --name admin --password 'xxxx'  --admin --email admin@bytle.net
New user 'admin' has been successfully created!
```
  * Login to the interface

## FYI

  * [installation of Git on a server without Gogs](https://git-scm.com/book/en/v2/Git-on-the-Server-Setting-Up-the-Server)

## Migrate (export / import)

From [Doc](https://discuss.gogs.io/t/how-to-backup-restore-and-migrate/991)

  * Backup:
```bash
# This is important to go to the install root
# because the sqlite path is relative
cd /opt/gogs
./gogs backup --config=/opt/gogs/custom/conf/app.ini
```
```text
2020/09/29 11:23:07 [ INFO] Backup root directory: /tmp/gogs-backup-955766125
2020/09/29 11:23:07 [ INFO] Packing backup files to: gogs-backup-20200929112307.zip
2020/09/29 11:23:08 [ INFO] Dumping repositories in '/opt/gogs/gogs-repositories'
2020/09/29 11:23:21 [ INFO] Repositories dumped to: /tmp/gogs-backup-955766125/repositories.zip
2020/09/29 11:23:25 [ INFO] Backup succeed! Archive is located at: gogs-backup-20200929112307.zip
```

  * Restore
```bash
cd /opt/gogs
mv /tmp/gogs-backup-20200929112307.zip .
./gogs restore --from="gogs-backup-20200929112307.zip" --config=/opt/gogs/custom/conf/app.ini
```
```text
2020/09/29 09:33:23 [ INFO] Restore backup from: gogs-backup-20200929112307.zip
2020/09/29 09:33:28 [ INFO] Restore succeed!
```
