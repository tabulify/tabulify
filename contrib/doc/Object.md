# Object 


## Instantiation

An object has only one exclusive role:
  * of metadata holder (data structure)
  * of manager (operations - an object with a sort of static class collection and an environment )
  
A metadata object has always a hierarchy and was created by its parents 
until the top object which is the runtime object (ie tabular).

For instance:
  * the nio path functions [resolve](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html#resolve-java.nio.file.Path-) and [resolveSibling](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html#resolveSibling-java.nio.file.Path-) violate that rule.
  * the equivalent is the `DataPaths.childOf` and `DataPaths.siblingOf`

## Methods

The convention for the method name are:

  * get: retrieve a property
  * create: take arguments and process them with the object environment

