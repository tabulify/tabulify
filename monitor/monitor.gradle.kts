import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


description = "Monitoring"
val appName = "monitor"

val vertxVersion = rootProject.ext.get("vertxVersion").toString()
dependencies {

  implementation(project(":bytle-dns"))
  implementation(project(":bytle-vertx"))
  implementation(project(":bytle-smtp-client"))
  implementation("io.vertx:vertx-web-client:$vertxVersion")


}

plugins {
  // https://github.com/jponge/vertx-gradle-plugin
  id("io.vertx.vertx-plugin") version "1.2.0"
}

val monitorVerticle = "net.bytle.monitor.MonitorMain"

vertx {
  mainVerticle = monitorVerticle
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
        "Main-Verticle" to monitorVerticle,
        "Main-Command" to "run",
        "FatJar" to "yes",
        "Multi-Release" to "true"
      )
    )

  }

}

val deployTaskName = "deploy"
tasks.register(deployTaskName) {
  dependsOn(shadowJarTaskName)
  doLast {


    println("Fly Update Image")
    exec {
      commandLine(
        "fly",
        "machine",
        "update",
        "683d920b195638",
        "--yes",
        "--schedule",
        "daily",
        "--restart",
        "no",
        "--dockerfile",
        "./Dockerfile"
      )
    }

    println("Fly completed")

  }
}
