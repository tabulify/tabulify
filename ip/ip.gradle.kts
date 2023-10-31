description = "Ip geolocation"

val vertxVersion = rootProject.ext.get("vertxVersion").toString()
val scramClientVersion = rootProject.ext.get("scramClientVersion").toString()
val flywayVersion = rootProject.ext.get("flywayVersion").toString()

dependencies {

  /**
   * Base
   */
  implementation(project(":bytle-type"))
  implementation(project(":bytle-vertx"))
  testImplementation(testFixtures(project(":bytle-vertx")))

  /**
   * Component
   */
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("io.vertx:vertx-web-openapi:$vertxVersion")
  // Sql
  implementation("org.flywaydb:flyway-core:$flywayVersion")
  implementation("io.vertx:vertx-pg-client:$vertxVersion")
  implementation("com.ongres.scram:client:$scramClientVersion") // Postgres Optional dependency that is not so optional


}
