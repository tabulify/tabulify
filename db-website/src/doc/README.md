# Doc


## Create container

```dos
# cd this directory
# then
docker run ^
    --name bdoc ^
    -d ^
    -p 80:80 ^
    -v %cd%:/var/www/html/data ^
    gerardnico/dokuwiki:2018-04-22b
```



Then:
   * go to http://localhost/install.php
       * If the page takes to much time, be sure that you are not listening on the xDebug port and that a debug session is started :)
   * set up an admin user.
   * install the following plugins:
      * minimap
      * webcomponent
      * wrap
   * config:
     * https://www.dokuwiki.org/config:useheading - always

## Start the container

```dos
docker start bdoc
```