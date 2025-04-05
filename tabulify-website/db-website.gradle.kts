dependencies {
  implementation(project(":bytle-db"))
  implementation(project(":bytle-db-cli"))
  implementation(project(":bytle-cli"))
  implementation(project(":bytle-doctest"))
}

description = "Website documentation"

val currentDirectory: String by project
val arguments: String by project

tasks.register<JavaExec>("docrun") {
  main = "net.bytle.db.doc.DocRun"
  classpath = sourceSets["main"].runtimeClasspath
  if (project.hasProperty("currentDirectory")) {
    workingDir = File(currentDirectory)
  }
  if (project.hasProperty("arguments")) {
    /**
     * We split and escape the arguments to not have any file wildcard expansion
     * (ie star will not become a list of files)
     */
    args = arguments.split(" ").map { s -> "\"" + s + "\"" }
  }
}
