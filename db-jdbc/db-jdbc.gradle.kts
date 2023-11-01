val postgresVersion = rootProject.ext.get("postgresVersion").toString()

dependencies {
  api(project(":bytle-db"))
  api(project(":bytle-db-flow"))
  api(project(":bytle-log"))
  api(project(":bytle-type"))
  api(project(":bytle-regexp"))
  api("org.postgresql:postgresql:$postgresVersion")
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
