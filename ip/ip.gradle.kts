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

  /**
   * IpGelocation dependency
   */
  implementation("org.flywaydb:flyway-core:$flywayVersion")
  implementation("io.vertx:vertx-pg-client:$vertxVersion")
  implementation("com.ongres.scram:client:$scramClientVersion") // Postgres Optional dependency that is not so optional
  implementation(project(":bytle-db-jdbc")) // posgtres driver
  implementation(project(":bytle-db-csv")) // csv loading

  /**
   * Test
   */
  testImplementation("io.vertx:vertx-web-client:$vertxVersion")

}

plugins {
  //id("io.vertx.vertx-plugin")
  id("org.openapi.generator")
}

val openApiGroup = "openApi" // group in the outline

/**
 * This generator generates only the final openapi file in the resources directory
 * thanks to the .openapi-generator-ignore file that ignores all java files
 */
val openApiGenerateIp = "openapiGenerateIp"

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>(openApiGenerateIp) {

  group = openApiGroup
  generatorName.set("java-vertx-web")

  /**
   * The location of the spec file
   */
  inputSpec.set("$projectDir/src/main/openapi/ip-openapi.yaml")
  outputDir.set("$projectDir")

}

val openApi = "openapi"
val mainResourcesDir = "${projectDir}/src/main/resources"
tasks.register(openApi) {
  group = openApiGroup
  dependsOn(openApiGenerateIp)

  /**
   * Copy the openapi.yaml
   */
  doLast {
    ant.withGroovyBuilder {
      "move"(
        "file" to "$mainResourcesDir/openapi.yaml",
        "todir" to "$mainResourcesDir/openapi-spec-file/eraldy/ip"
      )
    }
  }
}
