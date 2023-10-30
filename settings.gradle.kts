pluginManagement {
  val vertxPluginVersion: String by settings
  plugins {
    // https://github.com/jponge/vertx-gradle-plugin
    id("io.vertx.vertx-plugin") version vertxPluginVersion
  }
}

/*
 * The settings files is the main entry to every project
 */
rootProject.buildFileName = "root.gradle.kts"
rootProject.name = "bytle-parent"


include(":bytle-cassandra")
include(":bytle-cli")
include(":bytle-db-transfer")
include(":bytle-log")
include(":bytle-type")
include(":bytle-crypto")
include(":bytle-command")
include(":bytle-doctest")
include(":bytle-fs")
include(":bytle-os")
include(":bytle-regexp")
include(":bytle-smtp-client")
include(":bytle-smtp-client-delivery")
include(":bytle-smtp-server")
include(":bytle-xml")
include(":bytle-sftp")
include(":bytle-db")
include(":bytle-db-assembly")
include(":bytle-db-cli")
include(":bytle-db-csv")
include(":bytle-db-excel")
include(":bytle-db-flow")
include(":bytle-db-flow-email")
include(":bytle-db-flow-template")
include(":bytle-db-gen")
include(":bytle-db-gen-entities")
include(":bytle-db-web-document")
include(":bytle-db-jdbc")
include(":bytle-db-json")
include(":bytle-db-mysql")
include(":bytle-db-oracle")
include(":bytle-db-parquet")
include(":bytle-db-sqlserver")
include(":bytle-db-sqlite")
include(":bytle-db-website")
include(":bytle-timer")
include(":bytle-http")
include(":bytle-template")
include(":bytle-zip")
include(":bytle-db-play")
include(":bytle-viz")
include(":bytle-vertx-openapi")
include(":bytle-test")
include(":bytle-db-yaml")
include(":bytle-base")
include(":bytle-vertx")
include(":bytle-tower-edge")
include(":bytle-tower")
include(":bytle-dns")
include(":bytle-web-document")
include(":bytle-monitor")
include(":bytle-s3")
include(":bytle-ip")

// we do that so that we can choose the gradle file by name
// may be there is a better way
rootProject.children.forEach { project ->
  project.projectDir = file(project.name.replace("bytle-", ""))
  project.buildFileName = "${project.projectDir.name}.gradle.kts"
  assert(project.projectDir.isDirectory)
  assert(project.buildFile.isFile)
}
