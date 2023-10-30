description = "Ip geolocation"

val vertxVersion = rootProject.ext.get("vertxVersion").toString()

dependencies {
  implementation(project(":bytle-vertx"))
}
