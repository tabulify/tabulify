/*
 */

dependencies {
  api(project(":bytle-type"))
  api(project(":bytle-db"))
  api(project(":bytle-log"))
  api(project(":bytle-db-csv"))
  // For the regexp generation
  api("com.github.curious-odd-man:rgxgen:1.2")
  testImplementation(project(":bytle-db-flow"))
}

description = "Db Gen"
