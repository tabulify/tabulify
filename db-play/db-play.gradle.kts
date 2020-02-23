plugins {
  id("application")
}

dependencies {
  compile("org.yaml:snakeyaml:1.20")
  compile(project(":bytle-type"))
  compile(project(":bytle-db"))
  compile(project(":bytle-fs"))
  compile(project(":bytle-db-jdbc"))
  compile(project(":bytle-db-sqlite"))
}

application {
  mainClassName = "net.bytle.db.play.DbPlay"
  executableDir = "bin"
}

tasks {
  "startScripts"(CreateStartScripts::class) {
    applicationName = "db-play"
  }
}
