val log4jVersion = "2.20.0"
// https://mvnrepository.com/artifact/org.assertj/assertj-core
val assertJAssertionVersion = "3.24.2"
val vertxVersion = "4.4.5"
val junit4Version = "4.13.2"
val junit5Version = "5.10.0"
val javaLanguageVersion = 11
val slf4jVersion = "2.0.6"
val jacksonVersion = "2.13.4"
// email SMTP server
// https://mvnrepository.com/artifact/com.github.davidmoten/subethasmtp
val subethaVersion = "6.0.7"
val jakartaEmailVersion = "2.0.1" // should be the same as in SimpleEmail (don't know how to do that)
val simpleEmailVersion =  "8.1.3"
// DnsJava
val dnsJavaVersion =  "3.5.2"

ext {
  set("vertxVersion", vertxVersion)
  set("jacksonVersion", jacksonVersion)
  set("subethaVersion", subethaVersion)
  set("simpleEmailVersion", simpleEmailVersion)
  set("jakartaEmailVersion", jakartaEmailVersion)
  set("dnsJavaVersion", dnsJavaVersion)
}

/**
 * For all projects
 */
allprojects {

  group = "net.bytle"

  // version is only specified when a release is made
  // https://docs.gradle.org/current/userguide/build_lifecycle.html#sec:project_evaluation
  // gradle -Pversion=1.0.1 distZip
  beforeEvaluate {
//    if (version.toString() == "unspecified") {
//      // We don't add the time because otherwise gradle will create a jar each time
//      version = "snapshot"
//    }
  }

}

/**
 * https://docs.gradle.org/current/userguide/plugins.html
 */
plugins {
  id("java")
  id("java-library")
  // https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures
  // https://stackoverflow.com/questions/65231626/gradle-test-library-in-multi-module-project
  id("java-test-fixtures") // Produces test fixtures (test jars)
  id("idea") // optional (to generate IntelliJ IDEA project files)
  id("eclipse") // optional (to generate Eclipse project files)
  id("com.github.johnrengelman.shadow") version "7.1.2" apply false
  // id("jacoco") // code coverage, https://github.com/jacoco/jacoco
}


subprojects {


  apply {
    plugin("java")
    plugin("java-library")
    plugin("java-test-fixtures")
    plugin("idea")
  }


  // https://docs.gradle.org/current/userguide/idea_plugin.html
  idea {
    module {
      isDownloadJavadoc = true
      isDownloadSources = true
    }
  }


  java {

    // https://docs.gradle.org/current/userguide/toolchains.html
    // Consistent build
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(javaLanguageVersion))
      //vendor.set(JvmVendorSpec.AMAZON) // not found in 7.5.1
    }

  }

  /**
   * Doc: https://docs.gradle.org/current/userguide/declaring_repositories.html
   */
  repositories {

    mavenCentral()

    // for GitHub address, should be before the edu repo
    maven {
      url = uri("https://jitpack.io")
    }
    maven {
      // for oracle jdbc
      // url = "https://repo.boundlessgeo.com/main/"
      url = uri("https://maven.icm.edu.pl/artifactory/repo/")
    }

  }

  dependencies {

    //"testCompile"("com.github.stefanbirkner:system-rules:1.17.0")
    // slf4j provides jdk, we may switch as we use log4j for the analytics endpoint
    implementation("org.slf4j:slf4j-jdk14:$slf4jVersion")
    // Used by event handler storage (analytics,csp,weblog,error) to write event to file
    // slf4j is now hooked to the JDK logger and not log4j
    // https://logging.apache.org/log4j/2.x/maven-artifacts.html
    // see log4j.xml
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")

    /**
     * Test
     * testImplementation: Implementation dependencies are not leaked to consumers when building
     * testFixtures
     * testFixturesApi: API dependencies are visible to consumers when building
     * testFixturesImplementation: Implementation dependencies are not leaked to consumers when building
     * https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures
     */

    testImplementation("org.assertj:assertj-core:$assertJAssertionVersion")

    // Deprecated for assertJ
    testImplementation("org.hamcrest:hamcrest-all:1.3")


    /**
     * Allow Junit 4 and Junit 5 test
     * https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
     * https://github.com/junit-team/junit5-samples/blob/main/junit5-migration-gradle/build.gradle
     */
    testImplementation(platform("org.junit:junit-bom:$junit5Version"))

    testImplementation("org.junit.jupiter:junit-jupiter") {
      because("allows to write and run Jupiter tests")
    }

    /**
     * A fixtures because needed by Vertx with the RunWith
     * ie @RunWith(VertxUnitRunner.class)
     */
    testFixturesApi("junit:junit:$junit4Version")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine") {
      because("allows JUnit 3 and JUnit 4 tests to run")
    }

    testRuntimeOnly("org.junit.platform:junit-platform-launcher") {
      because("allows tests to run from IDEs that bundle older version of launcher")
    }


  }


  // From
  // https://docs.gradle.org/current/samples/sample_jvm_multi_project_with_toolchains.html
  // When using toolchain, the source and target compatibility
  // should be defined in the compile phase, otherwise
  // there is a conflict/error thrown
  tasks.withType<JavaCompile>().configureEach {
    /**
     * By default, gradle takes the encoding of the os (CP15252)
     * Because we add UTF-8 character in the code, we overwrite this
     * https://docs.gradle.org/current/dsl/org.gradle.api.tasks.compile.CompileOptions.html#org.gradle.api.tasks.compile.CompileOptions:encoding
     */
    options.encoding = "UTF-8"
    javaCompiler.set(javaToolchains.compilerFor {
      languageVersion.set(JavaLanguageVersion.of(javaLanguageVersion))
    })
  }

  // https://docs.gradle.org/current/userguide/java_testing.html#using_junit5
  tasks.named<Test>("test") {

    // Gradle Run the test with JUnit5
    // If useJUnitPlatform is not present: No tests found for given
    useJUnitPlatform {
      includeEngines("junit-vintage") // run test in Junit4 format
      includeEngines("junit-jupiter") // run test in Junit5 format
    }

  }

}

// samples/testing/testReport in the full Gradle distribution
// doc: https://docs.gradle.org/current/userguide/java_testing.html#test_reporting
// The problem with this publication is that if there is an error in the test you don"t get anything
