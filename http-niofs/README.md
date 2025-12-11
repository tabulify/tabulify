# HTTP NIO File System

## About

This is the HTTP module.

It provides a https FileSystem NIO.

## How does it work

When you create a file system with the following URL: https://example.com/home

* the working directory is: `/home`
* and relative path of:  `home` would
  * yield the absolute path `/home/home`
  * yield the URI: https://example.com/home/home

When you create a file system with the following URL: https://example.com/home?prop=value

* the working directory is: `/home`
* and relative path of:  `home` would
  * yield the absolute path `/home/home`
  * yield the URI: https://example.com/home/home?prop=value


## Limitations

### Time

* `last modified time` exists with the  `Last-Modified` header
* `Last Access Time` - No standard header exists for when a resource was last accessed
* `Creation Time` - No standard header exists for when a resource was originally created

# Contrib

See [Contrib](contrib/contrib.md)
