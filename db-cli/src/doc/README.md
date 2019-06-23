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
   * set up an admin user.
   * install the following plugins:
      * minimap
      * backlinks
      * wrap

## Start the container

```dos
docker start bdoc
```