# Tabulify Website


## Start

### The stick

See the stick.md file in the combo dev repo.

```cmd
C:\DokuWikiStick\run.cmd
```

### Or The container

#### Create the container

  * cd in the [doc directory](src/doc)
```dos
cd src/doc
```
  * Create a container
```dos
docker run ^
    --name tabul ^
    -d ^
    -p 80:80 ^
    -v %cd%/pages:/var/www/html/data/pages ^
    -v %cd%/media:/var/www/html/data/media ^
    gerardnico/dokuwiki:2020-07-29
```


Then:
   * go to http://localhost/install.php
       * If the page takes to much time, be sure that you are not listening on the xDebug port and that a debug session is started :)
   * set up an admin user.
   * install the following plugins:
      * combo
      * wrap
   * config:
     * https://www.dokuwiki.org/config:useheading - always

#### Start the container

```bash
docker start tabul

# go inside
docker exec -ti tabul /bin/bash
```

## Run the command in the pages

[docrun](docrun.cmd) is a utility that permits to run the pages and
update the `console` block code of an `unit` with the command in the `code`

Syntax:
```bash
docrun page:id  [nocache]
```

It's the class [DocRun](src/main/java/net/bytle/db/doc/DocRun.java) run by `Gradle` to compile on the fly.

## Release

The pages are uploaded with `upssh`

  * Create a `.env` file at [doc](src/doc) with the following
```properties
UPSSH_SFTP_SERVER=xxxx
UPSSH_SFTP_USER=xxxx
UPSSH_SFTP_PASSWORD=xxxxx
UPSSH_SFTP_PORT=22
```
  * Call `upssh`
```bash
cd src/doc
upssh
```
