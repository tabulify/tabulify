# Sqlite

## Requirement   
  * Minimum Version must be [3.25](https://www.sqlite.org/draft/releaselog/3_25_0.html) for the window functions.

## Query Change  
  * Date were changed to use the sqlite date function because Sqlite does not support
    * `cast as date` function
    * `date +1 days` calculation 
  * `stddev_samp` function does not exist - [extension-functions.c](https://sqlite.org/contrib) has mathematical functions
  * `RIGHT` and `FULL OUTER` JOINs are not supported
  * `except` query 87
  
Super-aggregate not supported:
  * `rollup` clause were suppressed
  * `grouping` function does not exist - [Oracle grouping](https://docs.oracle.com/cd/B28359_01/server.111/b28286/functions064.htm) - Using the GROUPING function, you can distinguish a null representing the set of all values in a superaggregate row from a null in a regular row.
  
  



