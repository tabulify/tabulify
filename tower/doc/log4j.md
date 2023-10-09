# Log4j


We use log4j because:
* it gave a simple way to write analytics log event file with a rolling fashion.
* it didn't force you to pass a class name as log name as Sl4j does (When you copy the logger line to another class, it works out-of-the-box)

Log4j is now pretty well-used everywhere to define
the log behavior.


## How it works?

* The manager log4 class is the class called [Log4JManager](../src/main/java/net/bytle/tower/util/Log4JManager.java)
* It's static configuration function:
  * define the  [Log4JConfigurationFactory](../src/main/java/net/bytle/tower/util/Log4jConfigurationFactoryPlugin.java)
  * that calls the [Log4JConfiguration](../src/main/java/net/bytle/tower/util/Log4JConfiguration.java)
  * that
    * receive the `Appenders` from the [log4j.xml](../src/main/resources/log4j2.xml) configuration file
    * and create dynamically the loggers configuration in its `doConfigure` function

## Why not creating the Appenders programmatically ?

* Creating `Appenders` programmatically that uses lookup variable is really difficult in Log4j because the builder are only for static parameters (You need to define the node and even after that you will still miss the parent node as you can get one from a builder)
