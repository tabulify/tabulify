# Parquet


## Library
The implementation of Parquet in Java is called [Parquet Mr](https://github.com/apache/parquet-mr)

There is a [cli](https://github.com/apache/parquet-mr/blob/master/parquet-cli/README.md)
with csv support.


## Note
Don't forget [WinUtils on Windows](https://github.com/steveloughran/winutils/tree/master/hadoop-2.8.3/bin) - See [code](https://github.com/apache/hadoop/blob/release-2.7.1/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/Shell.java#L371)


## Code 
CSV to parquet experimentation which does not work

### Data
The data comes from:
https://github.com/Parquet/parquet-compatibility/tree/master/parquet-testdata/tpch

### Write
Example of output from a spark parquet write.
 
Parquet write is done with Catalyst schema:
```javascript
{
  "type" : "struct",
  "fields" : [ {
    "name" : "created_at",
    "type" : "string",
    "nullable" : true,
    "metadata" : { }
  }, {
    "name" : "tracking_id",
    "type" : "string",
    "nullable" : true,
    "metadata" : { }
  }, {
    "name" : "type",
    "type" : "string",
    "nullable" : true,
    "metadata" : { }
  }, {
    "name" : "user_id",
    "type" : "string",
    "nullable" : true,
    "metadata" : { }
  }, {
    "name" : "utm_campaign",
    "type" : "string",
    "nullable" : true,
    "metadata" : { }
  }, {
    "name" : "utm_medium",
    "type" : "string",
    "nullable" : true,
    "metadata" : { }
  } ]
}
```
and a corresponding Parquet message type. 
```
message spark_schema {
  optional binary created_at (UTF8);
  optional binary tracking_id (UTF8);
  optional binary type (UTF8);
  optional binary user_id (UTF8);
  optional binary utm_campaign (UTF8);
  optional binary utm_medium (UTF8);
}
```
They are also called schema. See:
   * [customer.schema](src/test/resources/tpch/customer.schema)
   * [nation.schema](src/test/resources/tpch/nation.schema)

## Others

  * Hadoop Native Windows Library comes from: https://github.com/karthikj1/Hadoop-2.7.1-Windows-64-binaries
 
