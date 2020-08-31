
plugins {
  application
}

application {
  applicationName = "tabli"
  mainClassName = "net.bytle.db.cli.Db"
}

tasks.register<CreateStartScripts>("tabulStartScripts") {
  outputDir = file("build/scripts")
  applicationName = "tabul"
  mainClassName = "net.bytle.db.play.DbPlayMain"
}

tasks.register<Zip>("dist"){
  dependsOn("tabulStartScripts","distZip")
}

tasks.register<JavaExec>("tabul") {
  main = "net.bytle.db.play.DbPlayMain"
  classpath = sourceSets["main"].runtimeClasspath
}


dependencies {
  compile(project(":bytle-db"))
  compile(project(":bytle-cli"))
  compile(project(":bytle-db-gen"))
  compile(project(":bytle-regexp"))
  compile(project(":bytle-log"))
  compile(project(":bytle-fs"))
  compile(project(":bytle-xml"))
  compile(project(":bytle-timer"))
  compile(project(":bytle-db-jdbc"))
  compile(project(":bytle-db-csv"))
  compile(project(":bytle-db-json"))
  runtimeOnly(project(":bytle-db-sqlite"))
  runtimeOnly(project(":bytle-db-tpc"))
  testCompile(project(":bytle-db-tpc"))
}

//tasks.register<Run>("Run") {
//  group = "Welcome"
//  description = "Produces a world greeting"
//  message = "Hello"
//  recipient = "World"
//}

description = "Db Cli"
