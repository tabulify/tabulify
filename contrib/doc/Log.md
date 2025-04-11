# Log

## About

Log is managed by:
  * the SL4JF facade
  * and LOG4J has backend

Note: the bytle log project will be removed in favor of a migration to the SLF4J facade.

## Why ?

We have taken the SLF4J facade because:
  * the logger implementation is not shipped. (The user of a library may use a single logging system and have then a single configuration file)
  * the SLF4J API facade is already shipped with a lot of library
  * we don't want to be able to mix logger instantiation between SLF4J and the backend
  
We have taken LOG4J because:
  * of its documentation
  * it's the only documentation that talks about async logging.
  * it has also out of the box a cassandra appender.

 
