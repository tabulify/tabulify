
description = "bytle Db parquet"

dependencies {
  api(project(":bytle-db"))
  api("org.apache.hadoop:hadoop-common:2.7.3")
  api("org.apache.parquet:parquet-common:1.11.0")
  api("org.apache.parquet:parquet-encoding:1.11.0")
  api("org.apache.parquet:parquet-column:1.11.0")
  api("org.apache.parquet:parquet-hadoop:1.11.0")
}
