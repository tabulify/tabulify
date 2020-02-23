// https://stackoverflow.com/questions/57534469/how-to-change-this-gradle-config-to-gradle-kotlin-dsl
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("java")
  id("com.github.johnrengelman.shadow") version "5.2.0"
  id("maven")
}


val vertxVersion = "3.8.4"
val sourceCompatibility = JavaVersion.VERSION_1_8

// https://docs.gradle.org/5.2.1/userguide/java_library_plugin.html
// compileOnly = only for the compilation not for the test
dependencies {

  // Vert.x
  compile("io.vertx:vertx-core:$vertxVersion")
  compile("io.vertx:vertx-config:$vertxVersion") // Config management: https://vertx.io/docs/vertx-config/java/
  // Web
  compile("io.vertx:vertx-web:$vertxVersion")
  compile("io.vertx:vertx-web-client:$vertxVersion")
  compile("io.vertx:vertx-web-api-contract:$vertxVersion") // Used to verify that the request has the good structure
  // Database
  compile("io.vertx:vertx-jdbc-client:$vertxVersion")
  compile("org.hsqldb:hsqldb:2.5.0")
  //implementation "org.xerial:sqlite-jdbc:3.28.0"
  compile("org.flywaydb:flyway-core:6.1.1")
  compile(project(":bytle-db"))
  compile(project(":bytle-db-jdbc"))
  compile(project(":bytle-fs"))
  // File system
  compile(project(":bytle-http"))
  compile(project(":bytle-zip"))
  // Vertx service
  compile("io.vertx:vertx-service-proxy:$vertxVersion")
  compileOnly("io.vertx:vertx-codegen:$vertxVersion")
  annotationProcessor("io.vertx:vertx-codegen:$vertxVersion:processor")
  // <3.8.4
  // annotationProcessor "io.vertx:vertx-service-proxy:$vertxVersion:processor"
  annotationProcessor("io.vertx:vertx-service-proxy:$vertxVersion")
  // for the service health
  compile("io.vertx:vertx-health-check:$vertxVersion")
  compile("io.vertx:vertx-dropwizard-metrics:$vertxVersion")
  compile("io.vertx:vertx-circuit-breaker:$vertxVersion")
  // Test
  testImplementation("io.vertx:vertx-unit:$vertxVersion")

}

val nexusUsername: String by project
val nexusPassword: String by project
val nexusUrl: String by project

val mainClassName = "net.bytle.api.Launcher"
val watchForChange = "src/main/java/net/**/*"
val doOnChange = "./gradlew classes"
val mainVerticle = "net.bytle.api.MainVerticle"


tasks.named<ShadowJar>("shadowJar") {
  archiveBaseName.set("app")
  mergeServiceFiles("META-INF/services/io.vertx.core.spi.VerticleFactory")
  manifest {
    attributes(mapOf("Main-Class" to mainClassName))
    attributes(mapOf("Main-Verticle" to mainVerticle))
    attributes(mapOf("Main-Command" to "run"))
  }
}


// Is a dependence on compileJava
// https://docs.gradle.org/current/dsl/org.gradle.api.tasks.compile.JavaCompile.html
// the generated code is put in src/main/generated,
// which some integrated development environments like IntelliJ IDEA will automatically pick up on the classpath.
// https://github.com/bulivlad/vertx-codegen-plugin
// https://gitter.im/eclipse-vertx/vertx-users?at=5d8db754bf625112c0eb65e4
tasks.register<JavaCompile>("annotationProcessing") {
  group = "build"
  // codegen
  description = "Generates the stubs"
  options.setIncremental(false)
  source = sourceSets.main.get().java
  classpath = configurations.compileClasspath.get();
  destinationDir = project.file("src/main/generated")
  options.annotationProcessorPath = configurations.annotationProcessor.get()
  options.debugOptions.debugLevel = "source,lines,vars"
  options.compilerArgs = listOf(
    "-proc:only",
    "-processor", "io.vertx.codegen.CodeGenProcessor",
    "-Acodegen.output=${project.projectDir}/src/main"
  )
}


// Adding the generated map to the set of source directory
sourceSets {
  main {
    java {
      setSrcDirs(srcDirs.plus("src/main/generated"))
    }
  }
}

tasks.compileJava {
  targetCompatibility = JavaVersion.VERSION_1_8.toString()
  sourceCompatibility = JavaVersion.VERSION_1_8.toString()
  dependsOn(":annotationProcessing")
}

tasks.compileTestJava {
  options.compilerArgs.plus("-proc:none")
}


// Run args from the application id
// https://vertx.io/blog/automatic-redeployment-in-eclipse-ide/
// Same as: vertx run  --redeploy=src/**/* --launcher-class=net.bytle.api.Launcher --on-redeploy="./gradlew classes"
//run {
//  args = ["run", "--redeploy=$watchForChange", "--launcher-class=$mainClassName", "--on-redeploy=$doOnChange"]
//}

tasks.getByName<Upload>("uploadShadow") {
  repositories.withGroovyBuilder {
    "mavenDeployer" {
      "repository"("url" to "${nexusUrl}/repository/maven-releases/") {
        "authentication"(
          "userName" to nexusUsername,
          "password" to nexusPassword
        )
      }
      "snapshotRepository"(
        "url" to "${nexusUrl}/repository/maven-snapshots/") {
        "authentication"(
          "userName" to nexusUsername,
          "password" to nexusPassword)
      }
    }
  }
}

tasks.getByName<Upload>("uploadArchives") {
  repositories.withGroovyBuilder {
    "mavenDeployer" {
      "repository"("url" to "${nexusUrl}/repository/maven-releases/") {
        "authentication"(
          "userName" to nexusUsername,
          "password" to nexusPassword)
      }
      "snapshotRepository"("url" to "${nexusUrl}/repository/maven-snapshots/") {
        "authentication"(
          "userName" to nexusUsername,
          "password" to nexusPassword)
      }
    }
  }
}

