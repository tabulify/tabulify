// https://stackoverflow.com/questions/57534469/how-to-change-this-gradle-config-to-gradle-kotlin-dsl
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.text.SimpleDateFormat
import java.util.*

val flywayVersion = rootProject.ext.get("flywayVersion").toString()
val jacksonVersion = rootProject.ext.get("jacksonVersion").toString()
val antJschVersion = rootProject.ext.get("antJschVersion").toString()
val hashIdVersion = rootProject.ext.get("hashIdVersion").toString()
val caffeineVersion = rootProject.ext.get("caffeineVersion").toString()
val mapdbVersion = rootProject.ext.get("mapdbVersion").toString()

val sshAntTask = configurations.create("sshAntTask")

plugins {
  id("io.vertx.vertx-plugin")
  id("maven-publish")
  id("org.openapi.generator")
  id("org.flywaydb.flyway")
}

/**
 * vertx run
 * https://github.com/jponge/vertx-gradle-plugin
 */
val towerLauncher = "net.bytle.tower.MainLauncher"
val towerMainVerticle = "net.bytle.tower.MainVerticle"
// duplicate with the version in the vertx module
val vertxVersion = rootProject.ext.get("vertxVersion").toString()

vertx {
  mainVerticle = towerMainVerticle
  vertxVersion
  launcher = towerLauncher
}

/**
 * For test only
 */
flyway {
  url = "jdbc:postgresql://localhost:5433/postgres"
  user = "postgres"
  password = "welcome"
  sqlMigrationPrefix = "v"
  // https://flywaydb.org/documentation/configuration/parameters/table
  table = "version_log"
  flyway.cleanDisabled = false
}



val cspRealmsSchema = "cs_realms"

tasks.getByName<org.flywaydb.gradle.task.FlywayCleanTask>("flywayClean") {

  schemas = arrayOf(cspRealmsSchema)

}


tasks.register<org.flywaydb.gradle.task.FlywayMigrateTask>("flywayRealms") {

  // https://flywaydb.org/documentation/configuration/parameters/locations
  // locations = arrayOf("classpath:db/cs-tenant") # don't use that, classpath location has cache issue (the file is in the jar and not always updated)
  locations = arrayOf("filesystem:src/main/resources/db/cs-realms")
  schemas = arrayOf(cspRealmsSchema)
  // https://flywaydb.org/documentation/configuration/parameters/ignoreMigrationPatterns
  // ignore if a repeatable script is missing is only for the paid version
  // ignoreMigrationPatterns = arrayOf("repeatable:missing")

}

// https://docs.gradle.org/5.2.1/userguide/java_library_plugin.html
// compileOnly = only for the compilation not for the test
dependencies {


  implementation(project(":bytle-db"))
  implementation(project(":bytle-db-jdbc"))
  implementation(project(":bytle-fs"))
  implementation(project(":bytle-db-csv"))
  // File system
  implementation(project(":bytle-http"))
  implementation(project(":bytle-zip"))
  // Shares
  implementation(project(":bytle-vertx"))
  // To test email
  implementation(project(":bytle-dns"))

  // Web
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("io.vertx:vertx-web-client:$vertxVersion")
  implementation("io.vertx:vertx-web-openapi:$vertxVersion")
  // Health
  implementation("io.vertx:vertx-health-check:$vertxVersion")
  // Mail
  implementation("io.vertx:vertx-mail-client:$vertxVersion")
  implementation(project(":bytle-smtp-client"))
  // Sql
  // implementation "org.xerial:sqlite-jdbc:3.28.0"
  implementation("org.flywaydb:flyway-core:$flywayVersion")
  implementation("io.vertx:vertx-pg-client:$vertxVersion")
  implementation("com.ongres.scram:client:2.1") // Postgres Optional dependency that is not so optional

  // Auth
  // Authentication
  // not yet shared in the vertx module
  // because it needs refactoring to inject the auth function
  implementation("io.vertx:vertx-auth-common:$vertxVersion")
  implementation("io.vertx:vertx-auth-sql-client:$vertxVersion") // https://vertx.io/docs/vertx-auth-sql-client/java/
  implementation("io.vertx:vertx-auth-jwt:$vertxVersion") // Jwt
  implementation("io.vertx:vertx-auth-oauth2:$vertxVersion") // Oauth

  // id
  // https://mvnrepository.com/artifact/org.hashids/hashids
  implementation("org.hashids:hashids:$hashIdVersion")

  // Db
  implementation("org.mapdb:mapdb:$mapdbVersion")


  // Serialization of LocalDateTime
  // Java 8 date/time type `java.time.LocalDateTime` not supported by default:
  // add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"
  // to enable handling
  // https://vertx.io/docs/4.1.8/vertx-sql-client-templates/java/#_java_datetime_api_mapping
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

  // Cache
  implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")


  // WebDriver
  // https://www.selenium.dev/documentation/en/selenium_installation/installing_selenium_libraries/
  // https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
  testImplementation("org.seleniumhq.selenium:selenium-java:4.9.1")
  // https://mvnrepository.com/artifact/io.github.bonigarcia/webdrivermanager
  testImplementation("io.github.bonigarcia:webdrivermanager:5.3.3")

  // Wiser code
  testImplementation(testFixtures(project(":bytle-smtp-client")))

  sshAntTask("org.apache.ant:ant-jsch:$antJschVersion")

  // Test
  testImplementation(testFixtures(project(":bytle-vertx")))


}

val nexusUserName: String? by project
val nexusUserPwd: String? by project
val nexusUrl: String? by project


val shadowArchiveBaseName = "tower"
val shadowJarTaskName = "shadowJar"
tasks.named<ShadowJar>(shadowJarTaskName) {

  // if you want to change the name of the jar to tower-all.jar
  archiveBaseName.set(shadowArchiveBaseName)
  mergeServiceFiles()
  // Doc https://vertx.io/docs/vertx-core/java/#_using_the_launcher_in_fat_jars
  manifest {
    attributes(mapOf("Main-Class" to towerLauncher))
    attributes(mapOf("Main-Verticle" to towerMainVerticle))
    attributes(mapOf("Main-Command" to "run"))
    attributes["FatJar"] = "yes"
  }
}

// https://openapi-generator.tech/docs/plugins#gradle
// https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator-gradle-plugin/README.adoc
val apiGenerateServerCodeTaskName = "apiGenerateServerCode"
val eraldyDomainName = "eraldy"
val specResourcePrefix = "openapi-spec-file"
val apiAppName = "api"
// private/public are reserved java word, package name should be lowercase
// open-api generator does not support uppercase letter in the body for api prefix
val apiAppJavaName = "api"
val eraldyAppJavaPackagePath = "net.bytle.tower.${eraldyDomainName}"
val eraldyModelOpenApiJavaPackage = "net.bytle.tower.${eraldyDomainName}.model.openapi"
val openApiFileName = "openapi.yaml"
val openApiGroup = "OpenApi"
val mainResourcesDir = "${projectDir}/src/main/resources"
tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>(apiGenerateServerCodeTaskName) {

  group = openApiGroup

  /**
   * The name of the api
   */
  apiNameSuffix.set(apiAppJavaName)
  /**
   * The location of the spec file
   */
  inputSpec.set("$projectDir/src/main/openapi/${eraldyDomainName}-${apiAppName}-${openApiFileName}")
  /**
   * The location of the generated interface
   */
  apiPackage.set("${eraldyAppJavaPackagePath}.${apiAppJavaName}.openapi.interfaces")
  /**
   * The location of the classes that tied interface, implementer and vertx
   */
  invokerPackage.set("${eraldyAppJavaPackagePath}.${apiAppJavaName}.openapi.invoker")
  /**
   * The pojos (they are shared)
   */
  modelPackage.set(eraldyModelOpenApiJavaPackage)
  /**
   * The open-api config files
   */
  configFile.set("$projectDir/.openapi-generator-${eraldyDomainName}-${apiAppName}-config.yaml")

  /**
   * Common to API, vertx-based
   */
  outputDir.set("$projectDir")
  generatorName.set("java-vertx-web")
  templateDir.set("$projectDir/src/main/openapi/templates")

  /**
   * For inheritance, see openapi.md
   */
  openapiNormalizer.set(
    mapOf(
      "REF_AS_PARENT_IN_ALLOF" to "true"
    )
  )

  reservedWordsMappings.set(
    mapOf(
      "list" to "list"
    )
  )

  typeMappings.set(
    mapOf(
      "OffsetDateTime" to "LocalDateTime"
    )
  )

  // Import Mapping are now in the config file
  //  importMappings.set(
  //    mapOf(
  //      "java.time.OffsetDateTime" to "java.time.LocalDateTime",
  //      // Import Analytics objects from the common vertx module
  //      "AnalyticsEvent" to "net.bytle.vertx.analytics.model.AnalyticsEvent"
  //    )
  //  )

  // https://openapi-generator.tech/docs/generators/java-vertx-web
  val additionalProperties = mapOf(
    "dateLibrary" to "java8"
  )
  configOptions.set(additionalProperties)

  // https://openapi-generator.tech/docs/globals/
  // https://openapi-generator.tech/docs/debugging/#templates
  globalProperties.set(
    mapOf(
      // print the data model passed to template (not the api, pojo only)
      // "debugModels" to "true"
      // print the paths (api) passed to template (only the api, not the pojo)
      // "debugOperations" to "true"
    )
  )

}

/**
 * Generate only the internal pojos
 */
val generateModel = "ModelGenerate"
tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>(generateModel) {

  group = openApiGroup

  /**
   * The location of the spec file
   */
  inputSpec.set("$projectDir/src/main/openapi/${eraldyDomainName}-common-openapi.yaml")

  /**
   * Common is not a valid spec
   */
  validateSpec.set(false)

  /**
   * The pojos (they are shared)
   */
  modelPackage.set(eraldyModelOpenApiJavaPackage)

  /**
   * Common to API, vertx-based
   */
  outputDir.set("$projectDir")
  generatorName.set("java-vertx-web")
  templateDir.set("$projectDir/src/main/openapi/templates")

  /**
   * For inheritance, see openapi.md
   */
  openapiNormalizer.set(
    mapOf(
      "REF_AS_PARENT_IN_ALLOF" to "true"
    )
  )

  reservedWordsMappings.set(
    mapOf(
      "list" to "list"
    )
  )

  typeMappings.set(
    mapOf(
      "OffsetDateTime" to "LocalDateTime"
    )
  )
  importMappings.set(
    mapOf(
      "java.time.OffsetDateTime" to "java.time.LocalDateTime"
    )
  )

  // https://openapi-generator.tech/docs/generators/java-vertx-web
  val configs = mapOf(
    "dateLibrary" to "java8"
  )
  configOptions.set(configs)
  // https://openapi-generator.tech/docs/globals/
  // https://openapi-generator.tech/docs/customization#selective-generation
  globalProperties.set(
    mapOf(
      // Generate only models (ie POJO)
      // The value should be kept empty (true is not working)
      "models" to ""
      //, "debugModels" to "true"
    )
  )


}

/**
 * Generate the admin API server code and copy the openapi.yaml
 */
val openapiGenerateTaskName = "openapi"

tasks.register(openapiGenerateTaskName) {
  group = openApiGroup
  dependsOn(apiGenerateServerCodeTaskName)

  /**
   * Copy the openapi.yaml
   */
  doLast {
    ant.withGroovyBuilder {
      "move"(
        "file" to "${mainResourcesDir}/${openApiFileName}",
        "todir" to "${mainResourcesDir}/${specResourcePrefix}/${eraldyDomainName}/${apiAppName}"
      )
    }
  }
}


// No more used -- too complicated
// Is a dependence on compileJava
// https://docs.gradle.org/current/dsl/org.gradle.api.tasks.compile.JavaCompile.html
// the generated code is put in src/main/generated,
// which some integrated development environments like IntelliJ IDEA will automatically pick up on the classpath.
// https://github.com/bulivlad/vertx-codegen-plugin
// https://gitter.im/eclipse-vertx/vertx-users?at=5d8db754bf625112c0eb65e4
//tasks.register<JavaCompile>("annotationProcessing") {
//  group = "build"
//  // codegen
//  description = "Generates the stubs"
//  options.isIncremental = false
//  source = sourceSets.main.get().java
//  classpath = configurations.compileClasspath.get()
//  destinationDirectory.set(project.file("src/main/generated"))
//  options.annotationProcessorPath = configurations.annotationProcessor.get()
//  options.debugOptions.debugLevel = "source,lines,vars"
//  options.compilerArgs = listOf(
//    "-proc:only",
//    "-processor", "io.vertx.codegen.CodeGenProcessor",
//    "-Acodegen.output=${project.projectDir}/src/main"
//  )
//}


// Adding the generated map to the set of source directory
sourceSets {
  main {
    java {
      setSrcDirs(srcDirs.plus("src/main/generated"))
    }
  }
}

/**
 * Just run the application
 *
 * Note: Continuous build will not work because the Vertx
 * command will never give the process back to gradle
 * in order to watch the file system,
 *
 */
val runTowerTaskGroup = "runTower"
val currentDirectory: String by project
val runTower = "runTower"
tasks.register<JavaExec>(runTower) {
  group = runTowerTaskGroup
  mainClass.set("$mainClass")
  args = mutableListOf(
    "run",
    "--launcher-class=$towerLauncher",
    towerMainVerticle
  )
  classpath = sourceSets["main"].runtimeClasspath
  if (project.hasProperty("currentDirectory")) {
    workingDir = File(currentDirectory)
  }
  //dependsOn(processResources)
}

/**
 * Just a copy of the web file
 */
val javascriptProjectDir = "${project.projectDir}/src/main/javascript"
val javascriptCopyToWebRoot = "copyJavascript"
// process resources is called before test and for each test
//val processResources by tasks.getting(ProcessResources::class) {
//  dependsOn(javascriptCopyToWebRoot)
//}
tasks.register<Copy>(javascriptCopyToWebRoot) {
  from("${javascriptProjectDir}/build")
  destinationDir = File("${buildDir}/classes/java/main/webroot")
  //dependsOn(javascriptBuildFrontendTask)
}

val deployTaskName = "deploy"
tasks.register(deployTaskName) {
  dependsOn(shadowJarTaskName)
  doLast {

    // Variable
    val backendServerHost: String by project
    val backendServerPort: String by project
    val backendUserName: String by project
    val backendUserPwd: String by project
    val backendAppName = "tower"
    val backendAppHome = "/opt/apps/${backendAppName}"
    val backendAppArchive = "$shadowArchiveBaseName-all"

    /**
     * Upload the file
     */
    ant.withGroovyBuilder {
      "taskdef"(
        "name" to "scp",
        "classname" to "org.apache.tools.ant.taskdefs.optional.ssh.Scp",
        "classpath" to sshAntTask.asPath
      )
    }

    val timeStamp = SimpleDateFormat("yyyy.MM.dd-HH.mm.ss").format(Calendar.getInstance().time)
    val appFile = "${backendAppArchive}.jar"
    val deploymentFile = "build/libs/$appFile"
    val remoteDeploymentFile = "${backendAppArchive}-to-deploy-${timeStamp}.jar"

    println("Uploading the deployment file to ${backendAppHome}/$deploymentFile")
    // https://ant.apache.org/manual/Tasks/scp.html
    ant.withGroovyBuilder {
      "scp"(
        "file" to deploymentFile,
        "remoteTofile" to "${backendUserName}@${backendServerHost}:${backendAppHome}/$remoteDeploymentFile",
        "sftp" to "true",
        "port" to backendServerPort,
        "trust" to "yes",
        "password" to backendUserPwd,
        "verbose" to true,
        "failonerror" to true
      )
    }
    println("Upload completed")

    /**
     * Stop the service and rename the jar file
     */
    println("Restarting the service")
    // Add the class path for sshexec

    ant.withGroovyBuilder {
      "taskdef"(
        "name" to "sshexec",
        "classname" to "org.apache.tools.ant.taskdefs.optional.ssh.SSHExec",
        "classpath" to sshAntTask.asPath
      )
    }
    // https://ant.apache.org/manual/Tasks/sshexec.html
    // The echo done at the end is to make the command always successful
    val backupFile = "${backendAppArchive}-backup-${timeStamp}.jar"
    val command =
      "sudo systemctl stop $backendAppName ; mv ${backendAppHome}/$appFile ${backendAppHome}/${backupFile} ; mv ${backendAppHome}/$remoteDeploymentFile ${backendAppHome}/${appFile} ;  sudo systemctl start $backendAppName; echo Stop, Move and Start Done"
    ant.withGroovyBuilder {
      "sshexec"(
        "command" to command,
        "host" to backendServerHost,
        "username" to backendUserName,
        "port" to backendServerPort,
        "trust" to "yes",
        "password" to backendUserPwd,
        "verbose" to true,
        "failonerror" to true
      )
    }
    println("Restart completed")
  }
}
