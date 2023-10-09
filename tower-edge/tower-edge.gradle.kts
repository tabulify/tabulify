import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Tower Edge"

val projectVertxVersion = rootProject.ext.get("vertxVersion").toString()
//https://mvnrepository.com/artifact/net.lingala.zip4j/zip4j
val zip4jVersion = "2.11.5"
//https://mvnrepository.com/artifact/software.amazon.awssdk/s3
val awsSdkVersion = "2.20.150"
//https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
//https://mvnrepository.com/artifact/com.fasterxml.jackson.dataformat/jackson-dataformat-xml
val jacksonVersion = "2.15.2"

val edgeVerticle = "net.bytle.edge.EdgeVerticle"

plugins {
  // https://github.com/jponge/vertx-gradle-plugin
  id("io.vertx.vertx-plugin") version "1.2.0"
}


dependencies {

  implementation(project(":bytle-base"))
  implementation(project(":bytle-type"))
  implementation(project(":bytle-vertx"))
  implementation("io.vertx:vertx-web:$projectVertxVersion")

  implementation("net.lingala.zip4j:zip4j:$zip4jVersion")
  implementation("software.amazon.awssdk:s3:$awsSdkVersion")
  implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
  testImplementation(testFixtures(project(":bytle-vertx")))

}

val vertxVersion = rootProject.ext.get("vertxVersion").toString()

vertx {
  mainVerticle = edgeVerticle
  vertxVersion
}

val vertxLauncher = "io.vertx.core.Launcher"
val shadowJarTaskName = "shadowJar"
tasks.named<ShadowJar>(shadowJarTaskName) {

  mergeServiceFiles()
  // Doc https://vertx.io/docs/vertx-core/java/#_using_the_launcher_in_fat_jars
  manifest {
    attributes(
      mapOf(
        "Main-Class" to vertxLauncher,
        "Main-Verticle" to edgeVerticle,
        "Main-Command" to "run",
        "FatJar" to "yes",
        "Multi-Release" to "true"
      )
    )

  }

}
