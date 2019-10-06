# Bytle Sftp with Groovy


## Introduction
How to use the ``Bytle Sftp`` library with [Groovy](http://www.groovy-lang.org/).

## Class Loader
Groovy has its own (library|class) loader. When using the ``%GROOVY_HOME%\bin\groovy(.bat)`` executable to run groovy script this can leverage problem.
 
**Why ?**

This is a problem for the static method [Paths.get(URI)](http://docs.oracle.com/javase/8/docs/api/java/nio/file/Paths.html#get-java.net.URI-)
as it search and loads the File System through the default Class Loader of Java and not the Groovy Class Loader.
 
The default classLoader will search the ``Bytle Sftp`` library in the classPath environment variable. Or the  ``%GROOVY_HOME%\bin\groovy(.bat)`` 
has in its classpath only one library: ``%GROOVY_HOME%\lib\groovy-X.X.X.jar``.
 
You can see this behaviour by calling the script [classLoading.groovy](classLoading.groovy):

  * with the ``%GROOVY_HOME%\bin\groovy(.bat)`` executable
  * and with your IDE (which normally add the ``Bytle Sftp`` in the classPath
  
### Solutions

In this directory, there is two solutions (and example):

  * loading the file System with the Groovy Class Loader and using it to create a path. No modifications are needed. See [traversing_method_1.groovy](traversing_method_1.groovy)
  * of modifying the ``%GROOVY_HOME%\bin\groovy(.bat)`` executable. See [traversing_method_2.groovy](traversing_method_2.groovy) that you execute with [traversing_method_2.bat](traversing_method_2.bat)



 
 