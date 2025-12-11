# Zip for working with archive file

* ZIP format dominance: ZIP is the most common archive format across platforms
* Simple, intuitive:
  * `zip` = compress/package,
  * `unzip` = decompress/unpackage

## Library

We use [compression-libraries](https://mvnrepository.com/open-source/compression-libraries)
because it's the most common [compression library](https://mvnrepository.com/open-source/compression-libraries)

## Note

### Cli tools

```bash
unzip world-db.zip -d /path/to/destination/
tar -xf world-db.zip
```

```ps
powershell -command "Expand-Archive -Path 'world-db.zip' -DestinationPath 'C:\extracted\'"
```

