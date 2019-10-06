# Bytle Sftp with Java


## Introduction
How to use the ``Bytle Sftp`` library with Java


## Get an absolute path with a absolute URI Format

With [Paths](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Paths.html):

```java
URI uri = URI.create("sftp://myusername:mypassword@somehost:2222/tmp");
Path myPath = Paths.get(uri);

// And don't forget to close the file system when you don't need it anymore
myPath.getFileSystem().close();
```

The id of a sftp file system is the combination of user, host, port. Then if you need a second file from the same file system, you can omit the password.
Example:
```java
URI uri = URI.create("sftp://myusername@somehost:2222/my/Other/Absolute/Path");
Path mySecondPath = Paths.get(uri);
```
You can also just ask the file system from the first path `myPath` and then create a new path (even a relative one). 
Example:
```java
Path mySecondPath = myPath.getFileSystem().getPath("myRelativePath");
```


## Get the Sftp File System

  * Through the installed provider
```java
for (FileSystemProvider fileSystemProvider : FileSystemProvider.installedProviders()) {
   if (SftpFileSystemProvider.SFTP_SCHEME.equals(fileSystemProvider.getScheme())) {
       sftpFileSystemProvider = fileSystemProvider;
   }
}
```
  * With a path
```java
FileSystem sftpFileSystem = Paths.get(URI.create("sftp:/")).getFileSystem();
```

Don't forget to close it when it's no more needed.
```java
sftpFileSystem.close();
```


## Get a relative path from a Working Directory

If the path of the URI is a directory, it becomes the [working directory](http://gerardnico.com/wiki/file_system/working_directory). 
When not defined, the working directory default normally to the user's home directory.

```java
URI uri = new URI("sftp://" + user + ":" + pwd + "@" + host + ":" + port + "/path/To/A/Directory);
FileSystem sftpFileSystem = FileSystemProvider.newFileSystem(uri, env);
Path path = sftpFileSystem.getPath("myRelativePath");
// Print the absolute path
System.out.println(path.toAbsolutePath().toString());
```
The absolute path print must be then `/path/To/A/Directory/myRelativePath`.


## Get the current Working Directory


The toAbsolutePath is important if you want to print it otherwise you get a blank character.
```java
Path workingDirectory = sftpFileSystem.getPath("").toAbsolutePath();
```