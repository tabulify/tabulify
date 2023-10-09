dependencies {
  api(project(":bytle-db"))
  api(project(":bytle-db-flow"))
  api(project(":bytle-log"))
  api(project(":bytle-type"))
  api(project(":bytle-regexp"))
  // from https://jdbc.postgresql.org/download.html
  // 42.2.16 is the current version of the driver. This is the driver you should be using.
  // It supports PostgreSQL 8.2 or newer and requires Java 6 or newer. It contains support for SSL and the javax.sql package.
  api("org.postgresql:postgresql:42.2.16")
  testImplementation(project(":bytle-test"))
  testImplementation(project(":bytle-db-gen"))
}

/**
 * Create a test jar
 * From:
 *   https://docs.gradle.org/current/userguide/multi_project_builds.html#sec:project_jar_dependencies
 *   https://stackoverflow.com/questions/53335892/using-testcompile-output-from-other-subproject-gradle-kotlin-dsl
 */
configurations {
  create("test")
}

tasks.register<Jar>("testJar") {
  dependsOn("testClasses")
  archiveBaseName.set("${project.name}-test")
  from(sourceSets["test"].output.classesDirs)
  from(sourceSets["test"].output.resourcesDir)
}

artifacts {
  add("test", tasks["testJar"])
}

description = "db-jdbc"
