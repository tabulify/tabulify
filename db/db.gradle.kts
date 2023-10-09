

dependencies {
  // Jgraph 1.5 requires Java 11
  api("org.jgrapht:jgrapht-core:1.4.0")
  api(project(":bytle-timer"))
  api(project(":bytle-log"))
  api(project(":bytle-type"))
  api(project(":bytle-crypto"))
  api(project(":bytle-regexp"))
  api(project(":bytle-fs"))
  api(project(":bytle-os"))
  api(project(":bytle-template"))
  api("org.ini4j:ini4j:0.5.4")

  // tpc
  api(project(":bytle-command"))
  api("io.airlift.tpch:tpch:0.9")
  // jitpack repo
  api("com.github.teradata:tpcds:c0541d3")
  // api("com.teradata.tpcds:tpcds:1.3-SNAPSHOT")
  api("com.google.guava:guava:28.1-jre")
}




description = "Db"
