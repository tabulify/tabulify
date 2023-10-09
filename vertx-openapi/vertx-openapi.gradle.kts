

description = "Vertx Open API"

val vertxVersion = "3.8.4"
val swaggerVersion = "2.1.6"
plugins {
  id("java-library")
}

dependencies {
  api("io.swagger.core.v3:swagger-annotations:$swaggerVersion")
  api("io.swagger.core.v3:swagger-core:$swaggerVersion")
  implementation("io.vertx:vertx-core:$vertxVersion")
  implementation("io.vertx:vertx-web:$vertxVersion")
  testImplementation("com.google.guava:guava:25.1-jre")
  testImplementation("io.vertx:vertx-unit:$vertxVersion")
  testImplementation("io.vertx:vertx-web-client:$vertxVersion")
}
