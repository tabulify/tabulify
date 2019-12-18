# Zip / Jar

## About



## History

  * The code comes from this [project](https://github.com/stain/nio-zipfs)
  * that comes form this [project](https://github.com/marschall/zipfilesystem-standalone) by Philippe Marschall.

##  File System 


### Creation 

The factory methods defined by the 
[java.nio.file.FileSystems](http://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystems.html) 
class can be used to create a `ZipFileSystem` for an existing ZIP file:

  * use file type detection

```java
Path zipfile = Paths.get("foo.zip");
FileSystem fs = FileSystems.newFileSystem(zipfile, null);
```

  * locate file system by the legacy ZIP URL syntax

```java
Map<String,?> env = Collections.emptyMap();
URI uri = URI.create("zip:file:/mydir/foo.zip");
FileSystem fs = FileSystems.newFileSystem(uri, env);
```

### Usage
Once a FileSystem is created then classes in the 
[java.nio.file package](http://docs.oracle.com/javase/7/docs/api/java/nio/file/package-summary.html)
can be used to access files in the zip/JAR file, eg:

```java
Path mf = fs.getPath("/META-INF/MANIFEST.MF");
InputStream in = mf.newInputStream();
```

See [Demo.java](src/test/java/net/bytle/niofs/zip) for more interesting usages.


# License

[Licence](LICENSE) is based on the 
[demo/nio/zipfs](http://hg.openjdk.java.net/jdk8u/jdk8u/jdk/file/default/src/share/demo/nio/zipfs) 
code from [OpenJDK 8](http://openjdk.java.net/projects/jdk8/) which has been distributed
with Open JDK since 7u20 under a [BSD 3-Clause license](LICENSE).

For details, see the file [LICENSE](LICENSE). 

Since OpenJDK 9, 
[zipfs is included in the main OpenJDK codebase](http://hg.openjdk.java.net/jdk9/dev/jdk/file/default/src/jdk.zipfs/share/classes/jdk/nio/zipfs), 
but now covered by the GPL license.

This code is only based on the OpenJDK 8 demo code,
and remains licensed under BSD 3-Clause, which makes it compatible with 
other open source licenses like Apache Software License 2.0,
provided you retain the notices in the files [NOTICE](NOTICE) and [LICENSE](LICENSE).  
