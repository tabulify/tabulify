description = "template engine"


dependencies {
  implementation(project(":bytle-log"))
  // bytle-type to get json library from the json template engine
  api(project(":bytle-type"))
  // to be able to select html template data path
  api(project(":bytle-web-document"))
  // for the json built-in engine
  // implementation(project(":bytle-db-json"))
  // template (thymeleaf, pebble)
  api("org.thymeleaf:thymeleaf:3.0.12.RELEASE")
  implementation("io.pebbletemplates:pebble:3.1.4")
}
