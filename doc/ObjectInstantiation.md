# Object Instantiation

## About

An object has only one role:
  * metadata holder
  * manager (operations - static class)
  
A metadata object can not instantiate an other object.

For instance:
  * the nio path functions [resolve](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html#resolve-java.nio.file.Path-) and [resolveSibling](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html#resolveSibling-java.nio.file.Path-) violate that rule.
  * the equivalent is the `DataPaths.childOf` and `DataPaths.siblingOf`

