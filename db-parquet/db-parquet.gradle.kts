
description = "bytle Db parquet"

dependencies {
  compile(project(":bytle-db"))
  compile("org.apache.hadoop:hadoop-common:2.7.3")
  compile("org.apache.parquet:parquet-common:1.11.0")
  compile("org.apache.parquet:parquet-encoding:1.11.0")
  compile("org.apache.parquet:parquet-column:1.11.0")
  compile("org.apache.parquet:parquet-hadoop:1.11.0")
}
