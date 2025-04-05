

description = "Db Csv"

dependencies {
  api(project(":bytle-db"))
  implementation(project(":bytle-db-flow"))
  implementation(project(":bytle-type"))
  implementation("org.apache.commons:commons-csv:1.10.0")
}
