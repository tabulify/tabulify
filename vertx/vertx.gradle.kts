
description="Vertx core and utility"
// https://mvnrepository.com/artifact/io.vertx/vertx-core
val vertxVersion = rootProject.ext.get("vertxVersion").toString()


dependencies {

  implementation(project(":bytle-fs"))
  implementation(project(":bytle-base"))
  implementation(project(":bytle-type"))

  // It seems that you don't need to add the version after declaring the platform
  // As Seen here https://how-to.vertx.io/web-session-infinispan-howto/
  // Commented due to IDE saying that there is
  // implementation(platform("io.vertx:vertx-stack-depchain:$projectVertxVersion"))


  // Vert.x
  api("io.vertx:vertx-core:$vertxVersion")

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
  // Auth
  // Authentication
  implementation("io.vertx:vertx-auth-common:$vertxVersion")
  // https://vertx.io/docs/vertx-auth-sql-client/java/
  implementation("io.vertx:vertx-auth-sql-client:$vertxVersion")
  // Jwt
  implementation("io.vertx:vertx-auth-jwt:$vertxVersion")
  // Oauth
  implementation("io.vertx:vertx-auth-oauth2:$vertxVersion")
  // Sql Client : Native Postgres
  implementation("io.vertx:vertx-pg-client:$vertxVersion")

  // Vertx service
  //  implementation("io.vertx:vertx-service-proxy:$projectVertxVersion")
  //  compileOnly("io.vertx:vertx-codegen:$projectVertxVersion")
  //  annotationProcessor("io.vertx:vertx-codegen:$projectVertxVersion")
  //  annotationProcessor("io.vertx:vertx-service-proxy:$projectVertxVersion")
  // for the service health
  implementation("io.vertx:vertx-health-check:$vertxVersion")
  implementation("io.vertx:vertx-dropwizard-metrics:$vertxVersion")
  implementation("io.vertx:vertx-circuit-breaker:$vertxVersion")


  // For test api to pass it on
  testImplementation("io.vertx:vertx-unit:$vertxVersion")
  testImplementation("io.vertx:vertx-junit5:$vertxVersion")
  testFixturesApi("io.vertx:vertx-unit:$vertxVersion") // junit 4
  testFixturesApi("io.vertx:vertx-junit5:$vertxVersion")
  testFixturesApi(project(":bytle-base"))

}
