
description = "Monitoring"

val vertxVersion = rootProject.ext.get("vertxVersion").toString()
dependencies {

  implementation(project(":bytle-dns"))
  implementation(project(":bytle-vertx"))
  implementation("io.vertx:vertx-web-client:$vertxVersion")
  implementation("io.vertx:vertx-mail-client:$vertxVersion")

}
