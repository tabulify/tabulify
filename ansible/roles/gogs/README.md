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

