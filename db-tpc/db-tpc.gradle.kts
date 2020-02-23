/*
 */

dependencies {
    compile(project(":bytle-db"))
    compile(project(":bytle-command"))
    compile(project(":bytle-log"))
    compile(project(":bytle-regexp"))
    compile("io.airlift.tpch:tpch:0.9")
    compile("com.teradata.tpcds:tpcds:1.3-SNAPSHOT")
    compile("com.google.guava:guava:28.1-jre")
    testCompile(project(":bytle-fs"))
    testCompile(project(":bytle-db-jdbc"))
    testCompile(project(":bytle-db-csv"))
    testCompile(project(":bytle-db-sqlite"))
}

description = "Db Tpc"
