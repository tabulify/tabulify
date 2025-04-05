
description = "Db Json"

dependencies {
  api(project(":bytle-type"))
  api(project(":bytle-db"))
  api(project(":bytle-log"))
  testImplementation("org.json:json:20201115")
  testImplementation(project(":bytle-http"))
}
