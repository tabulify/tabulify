// https://stackoverflow.com/questions/57534469/how-to-change-this-gradle-config-to-gradle-kotlin-dsl
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


// this version should also be changed manually in the plugin
// // https://github.com/gradle/gradle/issues/9830
// change also the flyway version plugin !
val flywayVersion = "9.7.0"
val jacksonVersion = rootProject.ext.get("jacksonVersion").toString()
val sshAntTask = configurations.create("sshAntTask")

plugins {
  // https://github.com/jponge/vertx-gradle-plugin
  id("io.vertx.vertx-plugin") version "1.2.0"
  id("maven-publish")
  // Does not support yarn3
  // https://github.com/node-gradle/gradle-node-plugin/issues/176
  // id("com.github.node-gradle.node") version "5.0.0"
  // https://plugins.gradle.org/plugin/org.openapi.generator
  id("org.openapi.generator") version "7.0.0"
  // Version is manual because https://github.com/gradle/gradle/issues/9830
  // https://documentation.red-gate.com/fd/first-steps-gradle-166985825.html
  // change also the flyway version library !
  id("org.flywaydb.flyway") version "9.7.0"
}

/**
 * vertx run
 * https://github.com/jponge/vertx-gradle-plugin
 */
val towerLauncher = "net.bytle.tower.MainLauncher"
val towerMainVerticle = "net.bytle.tower.MainVerticle"
// duplicate with the version in the vertx module
val projectVertxVersion = rootProject.ext.get("vertxVersion").toString()
vertx {
  mainVerticle = towerMainVerticle
  vertxVersion = projectVertxVersion
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


val csIpDbSchema = "cs_ip"
val cspRealmsSchema = "cs_realms"

tasks.getByName<org.flywaydb.gradle.task.FlywayCleanTask>("flywayClean") {

  schemas = arrayOf(csIpDbSchema, cspRealmsSchema)

}

// https://flywaydb.org/documentation/usage/gradle/#build-script-multiple-databases
tasks.register<org.flywaydb.gradle.task.FlywayMigrateTask>("flywayIp") {

  // https://flywaydb.org/documentation/configuration/parameters/locations
  // classpath does not work when called from here
  // locations = arrayOf("classpath:db/cs-ip") # don't use that, classpath location has cache issue (the file is in the jar and not always updated)
  locations = arrayOf("filesystem:src/main/resources/db/cs-ip")
  schemas = arrayOf(csIpDbSchema)

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

  implementation("com.ongres.scram:client:2.1") // Postgres Optional dependency that is not so optional
  // implementation "org.xerial:sqlite-jdbc:3.28.0"
  implementation("org.flywaydb:flyway-core:$flywayVersion")

  implementation(project(":bytle-db"))
  implementation(project(":bytle-db-jdbc"))
  implementation(project(":bytle-fs"))
  implementation(project(":bytle-db-csv"))
  // File system
  implementation(project(":bytle-http"))
  implementation(project(":bytle-zip"))
  // Shares
  implementation(project(":bytle-vertx"))
  implementation("io.vertx:vertx-web:$projectVertxVersion")
  implementation("io.vertx:vertx-web-client:$projectVertxVersion")
  implementation("io.vertx:vertx-health-check:$projectVertxVersion")
  // Mail
  implementation("io.vertx:vertx-mail-client:$projectVertxVersion")
  implementation(project(":bytle-smtp-client"))

  // id
  // https://mvnrepository.com/artifact/org.hashids/hashids
  implementation("org.hashids:hashids:1.0.3")


  // Serialization of LocalDateTime
  // Java 8 date/time type `java.time.LocalDateTime` not supported by default:
  // add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"
  // to enable handling
  // https://vertx.io/docs/4.1.8/vertx-sql-client-templates/java/#_java_datetime_api_mapping
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

  // mixpanel test
  implementation("com.mixpanel:mixpanel-java:1.5.2")

  // In-memory Cache (2.9.3 because version 3 is only Java 11 compatible and not 8)
  implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")


  // WebDriver
  // https://www.selenium.dev/documentation/en/selenium_installation/installing_selenium_libraries/
  // https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
  testImplementation("org.seleniumhq.selenium:selenium-java:4.9.1")
  // https://mvnrepository.com/artifact/io.github.bonigarcia/webdrivermanager
  testImplementation("io.github.bonigarcia:webdrivermanager:5.3.3")

  // Wiser code
  testImplementation(testFixtures(project(":bytle-smtp-client")))

  sshAntTask("org.apache.ant:ant-jsch:1.9.2")

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
val comboPrivateApiGenerateServerCodeTaskName = "ComboPrivateGenerateServerCode"
val eraldyDomainName = "eraldy"
val specResourcePrefix = "openapi-spec-file"
val comboPrivateApiName = "combo-private"
// private is a reserved java word
// package name should be lowercase
// open-api generator does not support uppercase letter in the body for api prefix
val comboPrivateApiJavaName = "comboprivateapi"
val eraldyAppJavaPackagePath = "net.bytle.tower.${eraldyDomainName}.app"
val eraldyModelOpenApiJavaPackage = "net.bytle.tower.${eraldyDomainName}.model.openapi"
val openApiFileName = "openapi.yaml"
val openApiGroup = "OpenApi"
val mainResourcesDir = "${projectDir}/src/main/resources"
tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>(comboPrivateApiGenerateServerCodeTaskName) {

  group = openApiGroup

  /**
   * The name of the api
   */
  apiNameSuffix.set(comboPrivateApiJavaName)
  /**
   * The location of the spec file
   */
  inputSpec.set("$projectDir/src/main/openapi/${eraldyDomainName}-${comboPrivateApiName}-${openApiFileName}")
  /**
   * The location of the generated interface
   */
  apiPackage.set("${eraldyAppJavaPackagePath}.${comboPrivateApiJavaName}.openapi.interfaces")
  /**
   * The location of the classes that tied interface, implementer and vertx
   */
  invokerPackage.set("${eraldyAppJavaPackagePath}.${comboPrivateApiJavaName}.openapi.invoker")
  /**
   * The pojos (they are shared)
   */
  modelPackage.set(eraldyModelOpenApiJavaPackage)
  /**
   * The open-api config files
   */
  configFile.set("$projectDir/.openapi-generator-${eraldyDomainName}-${comboPrivateApiName}-config.yaml")

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
  globalProperties.set(
    mapOf(
      // print the data model passed to template
      // "debugModels" to "true"
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
val comboPrivateApiGenerateTaskName = "ComboPrivateGenerate"

tasks.register(comboPrivateApiGenerateTaskName) {
  group = openApiGroup
  dependsOn(comboPrivateApiGenerateServerCodeTaskName)

  /**
   * Copy the openapi.yaml
   */
  doLast {
    ant.withGroovyBuilder {
      "move"(
        "file" to "${mainResourcesDir}/${openApiFileName}",
        "todir" to "${mainResourcesDir}/${specResourcePrefix}/${eraldyDomainName}/${comboPrivateApiName}"
      )
    }
  }
}

val publicApiGenerateServerCodeTaskName = "ComboPublicGenerateServerCode"
val publicApiInternalName = "combo-public"
// public is a reserved java word
// package name should be lowercase
// open-api generator does not support uppercase letter in the body
val publicApiJavaInternalName = "combopublicapi"
tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>(publicApiGenerateServerCodeTaskName) {

  group = openApiGroup

  /**
   * The name of the api in Java
   */
  apiNameSuffix.set(publicApiJavaInternalName)
  /**
   * The location of the spec file
   */
  inputSpec.set("$projectDir/src/main/openapi/${eraldyDomainName}-${publicApiInternalName}-${openApiFileName}")
  /**
   * The location of the generated interface
   */
  apiPackage.set("${eraldyAppJavaPackagePath}.${publicApiJavaInternalName}.openapi.interfaces")
  /**
   * The location of the classes that tied interface, implementer and vertx
   */
  invokerPackage.set("${eraldyAppJavaPackagePath}.${publicApiJavaInternalName}.openapi.invoker")
  /**
   * The pojos (they are shared with the private api)
   */
  modelPackage.set(eraldyModelOpenApiJavaPackage)
  /**
   * The open-api config files
   */
  configFile.set("$projectDir/.openapi-generator-${eraldyDomainName}-${publicApiInternalName}-config.yaml")

  /**
   * Common to API, vertx-based
   */
  outputDir.set("$projectDir")
  generatorName.set("java-vertx-web")
  templateDir.set("$projectDir/src/main/openapi/templates")

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
  // https://openapi-generator.tech/docs/generators/java-vertx-web
  val configs = mapOf(
    "dateLibrary" to "java8"
  )
  configOptions.set(configs)
  // https://openapi-generator.tech/docs/globals/
//  val globalOptionsConf = mapOf("generateAliasAsModel" to "true")
//  globalProperties.set(globalOptionsConf)

}

/**
 * Generate the admin API server code and copy the openapi.yaml
 */
val publicApiGenerateTaskName = "ComboPublicGenerate"
tasks.register(publicApiGenerateTaskName) {

  group = openApiGroup

  dependsOn(publicApiGenerateServerCodeTaskName)

  /**
   * Copy the openapi.yaml
   */
  doLast {
    ant.withGroovyBuilder {
      "move"(
        "file" to "${mainResourcesDir}/${openApiFileName}",
        "todir" to "${mainResourcesDir}/${specResourcePrefix}/${eraldyDomainName}/${publicApiInternalName}"
      )
    }
  }
}

val memberApiGenerateServerCodeTaskName = "MemberAppGenerateServerCode"
val memberApiInternalName = "member"
// Package should be lowercase
val memberApiJavaPackageInternalName = "memberapp"
// Unfortunately, the case in class name is not preserved in the generation of the class,
// ie the file with a suffix of MemberApp would create a file name Memberapp with a class MemberApp
// It should be lowercase then
val memberApiSuffixJavaPackageInternalName = memberApiJavaPackageInternalName
tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>(memberApiGenerateServerCodeTaskName) {


  group = openApiGroup

  /**
   * The name of the api in Java
   */
  apiNameSuffix.set(memberApiSuffixJavaPackageInternalName)
  /**
   * The location of the spec file
   */
  inputSpec.set("$projectDir/src/main/openapi/${eraldyDomainName}-${memberApiInternalName}-${openApiFileName}")
  /**
   * The location of the generated interface
   */
  apiPackage.set("${eraldyAppJavaPackagePath}.${memberApiJavaPackageInternalName}.openapi.interfaces")
  /**
   * The location of the classes that tied interface, implementer and vertx
   */
  invokerPackage.set("${eraldyAppJavaPackagePath}.${memberApiJavaPackageInternalName}.openapi.invoker")
  /**
   * The pojos (they are shared)
   */
  modelPackage.set(eraldyModelOpenApiJavaPackage)
  /**
   * The open-api config files
   */
  configFile.set("$projectDir/.openapi-generator-${eraldyDomainName}-${memberApiInternalName}-config.yaml")

  /**
   * Common to API, vertx-based
   */
  outputDir.set("$projectDir")
  generatorName.set("java-vertx-web")
  templateDir.set("$projectDir/src/main/openapi/templates")

  reservedWordsMappings.set(
    mapOf(
      "list" to "list"
    )
  )
  // https://openapi-generator.tech/docs/generators/java-vertx-web
  val configs = mapOf(
    "dateLibrary" to "java8"
  )
  configOptions.set(configs)
  // https://openapi-generator.tech/docs/globals/
//  val globalOptionsConf = mapOf("generateAliasAsModel" to "true")
//  globalProperties.set(globalOptionsConf)

}

/**
 * Generate the member API server code and copy the openapi.yaml
 */
val memberGenerateServerCodeTaskName = "MemberGenerate"
tasks.register(memberGenerateServerCodeTaskName) {

  group = openApiGroup

  dependsOn(memberApiGenerateServerCodeTaskName)

  /**
   * Copy the openapi.yaml
   */
  doLast {
    ant.withGroovyBuilder {
      "move"(
        "file" to "${mainResourcesDir}/${openApiFileName}",
        "todir" to "${mainResourcesDir}/${specResourcePrefix}/${eraldyDomainName}/${memberApiInternalName}"
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
