dependencies {
  compile(project(":bytle-db"))
  compile(project(":bytle-log"))
  compile(project(":bytle-type"))
  compile(project(":bytle-regexp"))
  testCompile("org.postgresql:postgresql:42.2.9")
}

description = "db-jdbc"
