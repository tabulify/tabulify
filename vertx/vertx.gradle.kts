
description="Vertx core and utility"
// https://mvnrepository.com/artifact/io.vertx/vertx-core
val vertxVersion = rootProject.ext.get("vertxVersion").toString()
val scramClientVersion = rootProject.ext.get("scramClientVersion").toString()
val flywayVersion = rootProject.ext.get("flywayVersion").toString()
val hashIdVersion = rootProject.ext.get("hashIdVersion").toString()
val jacksonVersion = rootProject.ext.get("jacksonVersion").toString()

dependencies {

  implementation(project(":bytle-fs"))
  implementation(project(":bytle-base"))
  implementation(project(":bytle-type"))
  implementation(project(":bytle-template"))

  // It seems that you don't need to add the version after declaring the platform
  // As Seen here https://how-to.vertx.io/web-session-infinispan-howto/
  // Commented due to IDE saying that there is
  // implementation(platform("io.vertx:vertx-stack-depchain:$projectVertxVersion"))

  // Platform (BOM imports)
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  // Vert.x
  api("io.vertx:vertx-core:$vertxVersion")

  // Monitoring Prometheus
  implementation("io.vertx:vertx-micrometer-metrics:$vertxVersion")
  // https://mvnrepository.com/artifact/io.micrometer/micrometer-registry-prometheus
  api("io.micrometer:micrometer-registry-prometheus:1.11.5")

  // other health check
  implementation("io.vertx:vertx-health-check:$vertxVersion")

  // Resilience (Rate limiting, ...)
  // https://how-to.vertx.io/resilience4j-howto/ (9k)
  // based on https://github.com/Netflix/Hystrix no more maintained
  implementation("io.vertx:vertx-circuit-breaker:$vertxVersion")

  // The below artifact should be added individually
  implementation("io.vertx:vertx-config:$vertxVersion") // Config management: https://vertx.io/docs/vertx-config/java/
  implementation("io.vertx:vertx-config-yaml:$vertxVersion") // yaml
  // Web
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("io.vertx:vertx-web-client:$vertxVersion")
  // OpenApi is the follower of contract (Used to verify that the request has the good structure)
  // https://vertx.io/docs/vertx-web-openapi/java/
  implementation("io.vertx:vertx-web-openapi:$vertxVersion")
  // Mail
  implementation("io.vertx:vertx-mail-client:$vertxVersion")
  implementation(project(":bytle-smtp-client"))

  /**
   * SQL and Schema Management
   */
  implementation("org.flywaydb:flyway-core:$flywayVersion")
  implementation("io.vertx:vertx-pg-client:$vertxVersion")
  implementation("com.ongres.scram:client:$scramClientVersion") // Postgres Optional dependency that is not so optional

  /**
   * IpGeolocation CSV loading
   */
  implementation(project(":bytle-db-jdbc")) // posgtres driver
  implementation(project(":bytle-db-csv")) // csv loading

  /**
   * Auth
   */
  implementation("io.vertx:vertx-auth-jwt:$vertxVersion") // Jwt

  // Serialization of LocalDateTime
  // Java 8 date/time type `java.time.LocalDateTime` not supported by default:
  // add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"
  // to enable handling
  // https://vertx.io/docs/4.1.8/vertx-sql-client-templates/java/#_java_datetime_api_mapping
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

  // Vertx service
  //  implementation("io.vertx:vertx-service-proxy:$projectVertxVersion")
  //  compileOnly("io.vertx:vertx-codegen:$projectVertxVersion")
  //  annotationProcessor("io.vertx:vertx-codegen:$projectVertxVersion")
  //  annotationProcessor("io.vertx:vertx-service-proxy:$projectVertxVersion")

  // Analytics
  // mixpanel test
  implementation("com.mixpanel:mixpanel-java:1.5.2")

  // https://mvnrepository.com/artifact/org.hashids/hashids
  implementation("org.hashids:hashids:$hashIdVersion")

  // For test api to pass it on
  testImplementation("io.vertx:vertx-unit:$vertxVersion")
  testImplementation("io.vertx:vertx-junit5:$vertxVersion")
  testFixturesApi("io.vertx:vertx-unit:$vertxVersion") // junit 4
  testFixturesApi("io.vertx:vertx-junit5:$vertxVersion")
  testFixturesApi(project(":bytle-base"))

}

plugins {
  id("org.flywaydb.flyway")
}

val csIpDbSchema = "cs_ip"
// https://flywaydb.org/documentation/usage/gradle/#build-script-multiple-databases
tasks.register<org.flywaydb.gradle.task.FlywayMigrateTask>("flywayIp") {

  // https://flywaydb.org/documentation/configuration/parameters/locations
  // classpath does not work when called from here
  // locations = arrayOf("classpath:db/cs-ip") # don't use that, classpath location has cache issue (the file is in the jar and not always updated)
  locations = arrayOf("filesystem:src/main/resources/db/cs-ip")
  schemas = arrayOf(csIpDbSchema)

}

tasks.getByName<org.flywaydb.gradle.task.FlywayCleanTask>("flywayClean") {

  schemas = arrayOf(csIpDbSchema)

}
