
dependencies {
  api(project(":bytle-db"))
  api(project(":bytle-db-json"))
  // Jgraph 1.5 requires java 11
  api("org.jgrapht:jgrapht-io:1.4.0")
  testImplementation(project(":bytle-db-csv"))
  // CSV: To test that the mime type is not lost when the targets are created from sources
  testImplementation(project(":bytle-db-sqlite"))
}
