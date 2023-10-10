
description = "Monitoring"

val vertxVersion = rootProject.ext.get("vertxVersion").toString()
dependencies {

  implementation(project(":bytle-dns"))
  implementation(project(":bytle-vertx"))
  implementation(project(":bytle-smtp-client"))
  implementation("io.vertx:vertx-web-client:$vertxVersion")


}
