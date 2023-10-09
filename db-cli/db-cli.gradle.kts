import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
  distribution
  id("com.github.johnrengelman.shadow")
  id("org.jreleaser") version "1.3.1"
}



description = "Db Cli"
val tabliApplicationName = "tabli"
val tabliReleaseFileName = "release-${tabliApplicationName}.properties"

/**
 * Target Os for the distributions
 */
val x64Windows = "x64Windows"
val x86Windows = "x86Windows"
val x64Linux = "x64Linux"
val x64Macos = "x64Macos"

/**
 * Task name
 */
val extractX86WindowsTaskName: String = "${x86Windows}ExtractJdk"
val extractX64WindowsTaskName = "${x64Windows}ExtractJdk"
val extractX64LinuxTaskName = "${x64Linux}ExtractJdk"
val extractX64MacosTaskName = "${x64Macos}ExtractJdk"

val createJavaRuntimeX86WindowsTaskName: String = "${x86Windows}CreateJavaRuntime"
val createJavaRuntimeX64WindowsTaskName = "${x64Windows}CreateJavaRuntime"
val createJavaRuntimeX64LinuxTaskName = "${x64Linux}CreateJavaRuntime"
val createJavaRuntimeX64MacosTaskName = "${x64Macos}CreateJavaRuntime"

/**
 * A Configuration by Jdk represents a group of artifacts and their dependencies.
 * https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html
 *
 * One by Target Platform because the download is started by the resolution
 * https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html#org.gradle.api.artifacts.Configuration:resolvedConfiguration
 *
 */
val x86JdkWindowsConf: Configuration by configurations.creating
x86JdkWindowsConf.description = x86Windows

val x64JdkWindowsConf: Configuration by configurations.creating
x64JdkWindowsConf.description = x64Windows

val x64JdkLinuxConf: Configuration by configurations.creating
x64JdkLinuxConf.description = x64Linux

val x64JdkMacosConf: Configuration by configurations.creating
x64JdkMacosConf.description = x64Macos

/**
 * Global Variable
 */
val archiveBaseName: String = "tabulify"
val currentDirectory: String by project
val arguments: String by project
val uberFatJarClassifier: String = "all"
val tabli: String = "tabli"


/**
 * Create Version File
 */
val releaseFile = tasks.register<Copy>("versionFile") {
  into(layout.buildDirectory)
  from("src/gradle/release.properties") {
    expand(
      "version" to version,
      "buildTime" to LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
      "buildCommit" to getGitHash(),
      "buildJavaVersion" to org.gradle.internal.jvm.Jvm.current(),
      "buildGradleVersion" to GradleVersion.current().version,
      "buildOsVersion" to System.getProperty("os.name"),
      "buildOsName" to System.getProperty("os.version"),
      "buildOsArch" to System.getProperty("os.arch")
    )
    eachFile {
      name = tabliReleaseFileName
    }
  }
}

/**
 *
 * We use Shadow Jar because it can merge service files (gradle do not by default, see below)
 *
 * The gradle doc `Create an uberjar / fatJar`
 * https://docs.gradle.org/current/userguide/working_with_files.html#sec:creating_uber_jar_example
 * does not work because it will not concatenate the service file for the provider.
 *
 * Note that the shadow plugin is specified also in the root.gradle.kts
 * otherwise idea have problem to import/find the class
 *
 * Doc to create a custom task
 * https://imperceptiblethoughts.com/shadow/custom-tasks/
 *

 */
val tabliShadowJarName = "tabli"
val tabliShadowJar = tasks.register<ShadowJar>("tabliShadowJar") {
  group = tabli
  archiveBaseName.set(tabliShadowJarName)
  mergeServiceFiles()
  manifest.inheritFrom(jar.manifest)
  manifest {
    // the description and version attribute are set in the jar task
    attributes["Main-Class"] = "net.bytle.db.tabli.Tabli"
    attributes["FatJar"] = "yes"
  }

  // Add the release file
  // [AppendTextFile](https://imperceptiblethoughts.com/shadow/configuration/merging/#appending-text-files)
  // is not to append text file](https://github.com/johnrengelman/shadow/issues/180)
  dependsOn(releaseFile)

  // add the source
  from(sourceSets.main.get().output)
  // add the release
  from(layout.buildDirectory.file(tabliReleaseFileName))

  // configurations property is specified to inform Shadow which dependencies to merge into the output
  // runtime = dependency
  configurations = listOf(project.configurations.runtimeClasspath.get())



}

//tasks.withType<ShadowJar>() {
//  mergeServiceFiles()
//}

// Doc: https://datacadamia.com/gradle/application_jre
// We didn't use the badass plugin because we add extra resources in the zip
// https://badass-runtime-plugin.beryx.org/releases/latest/


//https://docs.gradle.org/6.2/userguide/declaring_repositories.html#sec:supported_metadata_sources
repositories {

  // To download the JVM
  // https://github.com/corretto/corretto-11/releases
  val corretto = ivy {

    // the link can be seen in the release of github
    // https://github.com/corretto/corretto-11/releases
    url = uri("https://corretto.aws/downloads/resources")
    patternLayout {
      artifact("/[revision]/[organisation]-[module]-[revision]-[classifier].[ext]")
    }

    // This is required in Gradle 6.0+ as metadata file (ivy.xml) is mandatory.
    metadataSources { artifact() }

  }

  // Use correto only for amazon dependencies
  // https://docs.gradle.org/current/userguide/declaring_repositories.html#declaring_content_exclusively_found_in_one_repository
  exclusiveContent {
    forRepositories(corretto)
    filter { includeGroup("amazon") }
  }

}

/**
 * Return the last commit
 */
val getGitHash = fun(): String {
  val stdout = ByteArrayOutputStream()
  project.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
    standardOutput = stdout
  }
  return stdout.toString().trim()
}

/**
 * We get it to be able to read the archiveFileName
 */
val jar = tasks.getByName<Jar>("jar") {
  manifest {
    // https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html
    // https://docs.oracle.com/javase/8/docs/technotes/guides/versioning/spec/versioning2.html#wp89936
    attributes["Name"] = "net/bytle/db/tabli"
    attributes["Description"] = "The tabli command line tool"
    attributes["Package-Title"] = "Tabli"
    attributes["Package-Version"] = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE)
    attributes["Package-Vendor"] = "tabulify"
    attributes["Build-Time"] = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
    attributes["Build-Commit"] = getGitHash()
    attributes["Build-Java-Version"] = org.gradle.internal.jvm.Jvm.current()
    attributes["Build-Gradle-Version"] = GradleVersion.current().version
    attributes["Build-Os-Version"] = System.getProperty("os.version")
    attributes["Build-Os-Name"] = System.getProperty("os.name")
    attributes["Build-Os-Arch"] = System.getProperty("os.arch")
  }
}


/**
 * The tabli start scripts
 */
val tabliStartScripts = tasks.register<CreateStartScripts>("tabliStartScripts") {
  group = tabli
  outputDir = file("${buildDir}/scripts")
  applicationName = tabliApplicationName
  mainClass.set("net.bytle.db.tabli.Tabli")
  classpath = files(configurations.runtimeClasspath) + files(jar.archiveFileName)
  // Class Path was too long for Windows, we have changed the template
  // see the `tasks.withType<CreateStartScripts>` block
}

/**
 * Bug in windows: The input line is too long.
 * https://github.com/gradle/gradle/tree/master/subprojects/plugins/src/main/resources/org/gradle/api/internal/plugins
 */
tasks.withType<CreateStartScripts> {
  (windowsStartScriptGenerator as TemplateBasedScriptGenerator).template =
    resources.text.fromFile("src/gradle/windowsStartScript.bat")
}


/**
 * Needed in the dependency
 * (Before the dependencies please)
 * in order to upload the distribution
 * to the server for the release
 */
val sshAntTask = configurations.create("sshAntTask")

dependencies {
  api(project(":bytle-type"))
  api(project(":bytle-db"))
  api(project(":bytle-db-flow"))
  api(project(":bytle-cli"))
  api(project(":bytle-db-gen"))
  api(project(":bytle-regexp"))
  api(project(":bytle-log"))
  api(project(":bytle-fs"))
  api(project(":bytle-xml"))
  api(project(":bytle-db-yaml"))
  api(project(":bytle-smtp-client"))
  api(project(":bytle-timer"))
  api(project(":bytle-db-jdbc"))
  api(project(":bytle-db-csv"))
  api(project(":bytle-db-json"))
  api(project(":bytle-db-web-document"))
  api(project(":bytle-db-flow-template"))
  api(project(":bytle-sftp"))
  api(project(":bytle-http"))
  api(project(":bytle-db-sqlite"))
  api(project(":bytle-db-mysql"))
  api(project(":bytle-db-oracle"))
  api(project(":bytle-db-sqlserver"))
  testImplementation(project(":bytle-db-gen"))
  testImplementation(project(":bytle-db-jdbc", "test"))
  testImplementation(project(":bytle-test"))

  sshAntTask("org.apache.ant:ant-jsch:1.9.2")

  // For the corretto ivy repo defined above,
  // the dependency maps to the pattern:
  // [organisation]:[module]:[revision]:[classifier]@[ext]
  // In maven term: [group]:[artifact]:[version]:[classifier]@[ext]
  // version is the version of github
  // https://github.com/corretto/corretto-11/releases
  x86JdkWindowsConf("amazon:corretto:11.0.16.9.1:windows-x86-jdk@zip")
  x64JdkWindowsConf("amazon:corretto:11.0.16.9.1:windows-x64-jdk@zip")
  x64JdkMacosConf("amazon:corretto:11.0.16.9.1:macosx-x64@tar.gz")
  x64JdkLinuxConf("amazon:corretto:11.0.16.9.1:linux-x64@tar.gz")


}


/**
 * External script called with `tabli.cmd`
 * to be able to develop interactively
 */
tasks.register<JavaExec>("tabli") {
  group = tabli
  mainClass.set("net.bytle.db.tabli.Tabli")
  classpath = sourceSets["main"].runtimeClasspath
  if (project.hasProperty("currentDirectory")) {
    workingDir = File(currentDirectory)
  }
  if (project.hasProperty("arguments")) {
    /**
     * We split and escape the arguments to not have any file wildcard expansion
     * (ie star will not become a list of files)
     */
    args = arguments.split(" +".toRegex()).map { s -> "\"" + s + "\"" }
  }
}


/**
 * Fail fast Used in the release
 */
var failFast: String = project.properties.getOrDefault("failFast", "false") as String

val testTask = tasks.getByName<Test>("test")
testTask.failFast = failFast.toBoolean()


/**
 * Upload tabli on the server
 * Note: 2022-11-24: We deploy only the shadow jar (due to error in hash when using Jlink)
 */
tasks.register("deploy") {
//  dependsOn(testTask, distZip)
  //dependsOn(x64LinuxDistZipTaskName)
  dependsOn(tabliShadowJar)
  failFast = "true"

  doLast {

    // Variable (in ~/.gradle/gradle.properties)
    val backendServerHost: String by project
    val backendServerPort: String by project
    val appsUserName: String by project
    val appsUserPwd: String by project
    val tabulifyUploadDir = "/opt/apps/tabli/shadow" // project.properties.getOrDefault("tabulifyUploadDir", "/opt/apps/tabli/shadow") as String


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

    project.logger.lifecycle("Deploying (Uploading at $tabulifyUploadDir)")
// https://ant.apache.org/manual/Tasks/scp.html
//    val archiveName = getArchiveBaseName(x64Linux)
//    val archiveFullName = "${archiveName}.zip"
//    val file = "${buildDir}/distributions/$archiveFullName"
    val file = "${buildDir}/libs/$tabliShadowJarName.jar"
    ant.withGroovyBuilder {
      "scp"(
        "file" to file,
        "sftp" to "true",
        "todir" to "${appsUserName}@${backendServerHost}:${tabulifyUploadDir}",
        "port" to backendServerPort,
        "trust" to "yes",
        "password" to appsUserPwd,
        "verbose" to true
      )
    }

    // When it was the distribution that was deployed
    // Add the class path for sshexec
//    ant.withGroovyBuilder {
//      "taskdef"(
//        "name" to "sshexec",
//        "classname" to "org.apache.tools.ant.taskdefs.optional.ssh.SSHExec",
//        "classpath" to sshAntTask.asPath
//      )
//    }
//    val command =
//      "rm -r ${tabulifyUploadDir}/tabli; unzip ${tabulifyUploadDir}/${archiveFullName} ; mv ${tabulifyUploadDir}/${archiveName} tabli ; rm ${tabulifyUploadDir}//${archiveFullName} ; echo Done"
//    ant.withGroovyBuilder {
//      "sshexec"(
//        "command" to command,
//        "host" to backendServerHost,
//        "username" to appsUserName,
//        "port" to backendServerPort,
//        "trust" to "yes",
//        "password" to appsUserPwd,
//        "verbose" to true
//      )
//    }

  }

}

val distribution = tasks.register("distribution") {
//  dependsOn(testTask, distZip)
  dependsOn(allDistZip)
  failFast = "true"
  doLast {

    // Variable
    val backendServerHost: String by project
    val backendServerPort: String by project
    val tabulifyUserName: String by project
    val tabulifyUserPwd: String by project
    val tabulifyUploadDir =
      project.properties.getOrDefault("tabulifyUploadDir", "/opt/www/bytle/farmer.bytle.net") as String

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
// https://ant.apache.org/manual/Tasks/scp.html
    val file = "${buildDir}/distributions/${archiveBaseName}.zip"
    ant.withGroovyBuilder {
      "scp"(
        "file" to file,
        "sftp" to "true",
        "todir" to "${tabulifyUserName}@${backendServerHost}:${tabulifyUploadDir}",
        "port" to backendServerPort,
        "trust" to "yes",
        "password" to tabulifyUserPwd,
        "verbose" to true
      )
    }


  }
}

/**
 * JVM
 *
 * based on idea of:
 * https://stackoverflow.com/questions/23023069/gradle-download-and-unzip-file-from-url/34327202
 * https://stackoverflow.com/questions/52744893/how-do-i-create-distributions-with-different-dependencies-using-gradle-applicati
 */
// The task to download the JVMs
tasks.register(extractX86WindowsTaskName) {
  group = "jdk"
  extractJdk(x86JdkWindowsConf)
}


tasks.register(extractX64WindowsTaskName) {
  group = "jdk"
  extractJdk(x64JdkWindowsConf)
}


tasks.register(extractX64LinuxTaskName) {
  group = "jdk"
  extractJdk(x64JdkLinuxConf)
}


tasks.register(extractX64MacosTaskName) {
  group = "jdk"
  extractJdk(x64JdkMacosConf)
}


val getJdkDirectory = fun(osConf: Configuration): Directory {
  // Get the only JDK dependency
  if (osConf.dependencies.size != 1) {
    throw RuntimeException("Exactly One Jvm file dependency for the OS (${osConf.description}) should be defined")
  }
  val jdkDependency: DefaultExternalModuleDependency =
    osConf.dependencies.iterator().next() as DefaultExternalModuleDependency

  // Get the only JDK artifact
  // The resolution will download the files if not present
  val jdkArtifact = jdkDependency.artifacts.iterator().next()

  // Define the target directory
  return project.layout.buildDirectory.dir("jdk/${jdkArtifact.classifier}-${jdkDependency.version}").get()
}

/**
 * Return the directory location where the java runtime
 * is created
 */
val getJavaRuntimeTargetDirectory = fun(osConf: Configuration): Directory {

  val imageName = if (osConf.description != null) osConf.description!! else osConf.name

  return project.layout.buildDirectory.dir("image").get().dir(imageName)

}


/**
 * A Task to download and unzip a Corretto JVM
 *
 * Based on the `Create a custom task doc`
 * https://docs.gradle.org/current/userguide/custom_tasks.html
 * https://docs.gradle.org/current/userguide/more_about_tasks.html#sec:passing_arguments_to_a_task_constructor
 *
 * That extends Copy
 * https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Copy.html
 *
 * Qualified function from project such as project.zipTree is important otherwise you get this error
 * `
 * The constructor for type Db_cli_gradle.JvmTask should be annotated with @Inject.
 * `
 */

fun extractJdk(osConf: Configuration) {

  val targetDestinationDirectory = getJdkDirectory(osConf)

  // If not exist copy
  if ((!project.file(targetDestinationDirectory).exists())) {

    // The resolution will download the files if not present
    val jvmZipFile = osConf.resolve().iterator().next()
    println("${osConf.description} Jvm file to unzip: $jvmZipFile")

    // Unarchive doc https://docs.gradle.org/current/userguide/working_with_files.html#sec:unpacking_archives_example
    if (jvmZipFile == null) {
      throw RuntimeException("Unable to find a Jvm file dependency for the OS (${osConf.description})")
    }

    project.copy {

      val fileTree: FileTree = when (jvmZipFile.extension) {
        "zip" -> project.zipTree(jvmZipFile)
        "gz" -> project.tarTree(jvmZipFile)
        else -> null
      } ?: throw RuntimeException("The extension (${jvmZipFile.extension}) is unknown for the Jvm file")
      from(fileTree) {
        eachFile {
// delete the first directory as explained in the example 12
// https://docs.gradle.org/current/userguide/working_with_files.html#sec:unpacking_archives_example
          var segments = relativePath.segments.drop(1)
// macos has more than a root directory:
//   * the home is at amazon-correto-8/Contents/Home
//   * and there is third directory such as Contents/MacOS
          if (osConf.description == "macos") {
            if (segments.size >= 2 && segments[0] == "Contents" && segments[1] == "Home") {
              segments = segments.drop(2)
            } else {
              this.exclude()
            }
          }
          relativePath = RelativePath(true, *segments.toTypedArray())
        }
      }
      into(targetDestinationDirectory)
      println("${osConf.description} Jvm file Unzipped")
    }
  }


}


/**
 * Java Runtime Task
 */
//tasks.register(createJavaRuntimeX64WindowsTaskName) {
//  group = "javaRuntime"
//  dependsOn(extractX64WindowsTaskName)
//  createJavaRuntime(x64JdkWindowsConf)
//}
//tasks.register(createJavaRuntimeX86WindowsTaskName) {
//  group = "javaRuntime"
//  dependsOn(extractX86WindowsTaskName)
//  createJavaRuntime(x86JdkWindowsConf)
//}
//tasks.register(createJavaRuntimeX64LinuxTaskName) {
//  group = "javaRuntime"
//  dependsOn(extractX64LinuxTaskName)
//  createJavaRuntime(x64JdkLinuxConf)
//}
//tasks.register(createJavaRuntimeX64MacosTaskName) {
//  group = "javaRuntime"
//  dependsOn(extractX64MacosTaskName)
//  createJavaRuntime(x64JdkMacosConf)
//}

/**
 * Java Runtime Task
 */
tasks.register("listJreModules") {
  group = "javaRuntime"
  getJavaJreModuleDependency()
}



/**
 * Return the java module used
 * by the application
 * in a format adequate for Jlink
 */
fun getJavaJreModuleDependency(): String {

  val installationPath = getJavaToolChainInstallationPath()
  // import org.apache.tools.ant.taskdefs.condition.Os
  var jDepsExecutableFile = "jdeps.exe"
  if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
    jDepsExecutableFile = "jdeps"
  }
  val jDeps = file(installationPath.dir("bin").file(jDepsExecutableFile))
  if (!jDeps.exists()) {
    throw RuntimeException("Jdeps was not found at $jDeps")
  }

  /**
   * Exec
   */
  /**
   * Quirks
   * module-path: Because of dependencies, jdeps may search for jar that are not in order
   * We path them in the module-path
   * We couldn't find a way to have the module in dependency order
   */
  val modulePaths = ArrayList<String>()
  project.configurations.runtimeClasspath.get().files.forEach { file ->
    val path = file.parentFile.absolutePath.toString()
    // snake is the only one for now
    if (path.contains("snake")) {
      modulePaths.add(path)
    }
  }
  val modulePath = modulePaths.joinToString(";")
  val commandLineArgs = ArrayList<String>()
  commandLineArgs.add("--multi-release") // in a multi-release jar, use the 11
  commandLineArgs.add("11")
  commandLineArgs.add("--print-module-deps") // output the java module dependency for use in jlink
  commandLineArgs.add("--ignore-missing-deps") // ignore missing deps
  commandLineArgs.add("-quiet") //  no warning message to get a clean output
  commandLineArgs.add("--module-path")
  commandLineArgs.add(modulePath)
  // Add jars
  project.configurations.runtimeClasspath.get().files.forEach { file ->
    commandLineArgs.add(file.absolutePath.toString())
  }

  // ByteArrayOutputStream
  // import at the top is mandatory otherwise there is resolution conflict with the java plugin
  // See https://github.com/gradle/kotlin-dsl-samples/issues/548
  // ie
  // import java.io.ByteArrayOutputStream
  val output = ByteArrayOutputStream()
  try {
    project.exec {
      executable = jDeps.absolutePath
      args = commandLineArgs
      //store the output instead of printing to the console:
      standardOutput = output
    }
  } catch (e: Exception) {
    throw RuntimeException(
      "Error: ${e.message}\nError output: $output\nCommand line: ${jDeps.absolutePath} $commandLineArgs",
      e
    )
  }
  return output.toString().trim()
}

/**
 * Return the home of the java running
 * this build
 *
 * See https://docs.gradle.org/current/userguide/toolchains.html#sec:plugins
 */
fun getJavaToolChainInstallationPath(): Directory {
  val toolchain = project.extensions.getByType<JavaPluginExtension>().toolchain
  val service = project.extensions.getByType<JavaToolchainService>()
  val defaultLauncher = service.launcherFor(toolchain)
  return defaultLauncher.get().metadata.installationPath
}


/**
 * Create a minimal java Runtime with Jlink
 * (a small JRE)
 */
//fun createJavaRuntime(osConf: Configuration) {
//
//  val targetDirectory = getJavaRuntimeTargetDirectory(osConf)
//  if (!targetDirectory.asFile.exists()) {
//
//    // ByteArrayOutputStream
//    // import at the top is mandatory otherwise there is resolution conflict with the java plugin
//    // See https://github.com/gradle/kotlin-dsl-samples/issues/548
//    // ie
//    // import java.io.ByteArrayOutputStream
//    val output = ByteArrayOutputStream()
//    // https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Exec.html
//    val installationPath = getJavaToolChainInstallationPath()
//    // import org.apache.tools.ant.taskdefs.condition.Os
//    var jLinkExecutableFile = "jlink.exe"
//    if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
//      jLinkExecutableFile = "jlink"
//    }
//    val jLink = file(installationPath.dir("bin").file(jLinkExecutableFile))
//    if (!jLink.exists()) {
//      throw RuntimeException("Jlink was not found at $jLink")
//    }
//    val commandLineArgs = ArrayList<String>()
//    commandLineArgs.add("--no-header-files")
//    commandLineArgs.add("--no-man-pages")
//    commandLineArgs.add("--strip-debug")
//    commandLineArgs.add("--compress")
//    commandLineArgs.add("2")
//    commandLineArgs.add("--module-path") // the path to the jdk
//    commandLineArgs.add(getJdkDirectory(osConf).dir("jmods").asFile.absolutePath)
//    commandLineArgs.add("--add-modules")
//    commandLineArgs.add(getJavaJreModuleDependency())
//    commandLineArgs.add("--output")
//    commandLineArgs.add(targetDirectory.asFile.absolutePath)
//    try {
//      project.exec {
//        workingDir = installationPath.dir("bin").asFile.absoluteFile
//        executable = jLink.absolutePath
//        args = commandLineArgs
//        //store the output instead of printing to the console:
//        standardOutput = output
//      }
//    } catch (e: Exception) {
//      // handler
//      throw RuntimeException("Error: ${e.message}\nError output: $output\nJlink used: ${jLink.absolutePath}\nArgs used: $commandLineArgs")
//    }
//    println("Java Modular Image ${osConf.description} created (with the set of Java module needed for the application)")
//  }
//
//}


/**
 * Return the name of the file
 */
fun getArchiveBaseName(distribution: String? = null): String {
  return archiveBaseName +
    if (distribution != null) {
      "-" + distribution.toLowerCase()
    } else {
      ""
    }
//    if (version.toString() == "snapshot") {
//      ""
//      // + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))
//    } else {
//      version
//    }
}

/**
 * All `into` path should be relative to get a root directory
 * into the archive
 *
 * The archive needs to have a root directory
 * otherwise if a user uses the `zip` command
 * it will unzip all data into the current directory
 * and it can then mess the file system up
 * if the current directory is not empty
 *
 */

distributions {


  main {

    // archive name = baseName + version .(zip|tar)
    // we don't set the date in the version because version is used everywhere
    distributionBaseName.set(getArchiveBaseName())

    contents {

      into("bin") {
        from("${buildDir}/scripts") {
          fileMode = Integer.parseInt("755", 8) // or 493 which is 755 base 8 in base 10
          dirMode = Integer.parseInt("750", 8)
        }
      }
      // Copy the jar of the cli
      into("lib") {
        from(project.layout.buildDirectory.dir("libs").get().file(jar.archiveFileName))
      }
      // Copy the runtime dependencies
      into("lib") { //
        from(files(configurations.runtimeClasspath))
      }
      // into
      // https://docs.gradle.org/current/javadoc/org/gradle/api/file/CopySpec.html#into-java.lang.Object-
      into("resource/tpcds_query") {
        // from
        // https://docs.gradle.org/current/javadoc/org/gradle/api/file/CopySpec.html#from-java.lang.Object...-
        from("../db-jdbc/src/main/sql/tpcds")
      }
      into("resource/howto") {
        from("../db-website/src/doc/howto/")
      }
      into("resource/entity") {
        from("../db-gen-entities/src/main/resources/entity")
      }

      /**
       * Release file
       * https://docs.gradle.org/current/userguide/working_with_files.html#sec:filtering_files
       *
       * If error:
       * gradle --stacktrace distZip
       */
      into("") {
        from("build/release.properties")
      }

      into("") {
        from("src/gradle/README.md") {
          expand(
            "version" to version,
            "commandName" to tabliApplicationName
          )
        }
      }

    }
  }

  create(x64Windows) {

    distributionBaseName.set(getArchiveBaseName(x64Windows))

    contents {
      into("") {
        from(getJavaRuntimeTargetDirectory(x64JdkWindowsConf))
      }
      with(distributions.main.get().contents)
    }


  }

  create(x86Windows) {


    distributionBaseName.set(getArchiveBaseName(x86Windows))

    contents {
      into("") {
        from(getJavaRuntimeTargetDirectory(x86JdkWindowsConf))
      }
      with(distributions.main.get().contents)
    }


  }

  create(x64Linux) {

    distributionBaseName.set(getArchiveBaseName(x64Linux))

    contents {
      into("") {
        from(getJavaRuntimeTargetDirectory(x64JdkLinuxConf))
      }
      with(distributions.main.get().contents)
    }

  }

  create(x64Macos) {

    distributionBaseName.set(getArchiveBaseName(x64Macos))

    contents {
      into("") {
        from(getJavaRuntimeTargetDirectory(x64JdkMacosConf))
      }
      with(distributions.main.get().contents)
    }
  }

}

/**
 * Distribution Dependency
 */
val distDep = tasks.register("distDep")
  .get()
  .dependsOn(jar)
  .dependsOn(tabliStartScripts)
  .dependsOn(configurations.runtimeClasspath)

tasks.getByName("distZip")
  .dependsOn(distDep)

// A task to generate all distribution artifact
val x64LinuxDistZipTaskName = "${x64Linux}DistZip"
val allDistZip = tasks.register("allDistZip")
  .get()
  .dependsOn(tasks.getByName("distZip"))
  .dependsOn("${x86Windows}DistZip")
  .dependsOn("${x64Windows}DistZip")
  .dependsOn("${x64Macos}DistZip")
  .dependsOn(x64LinuxDistZipTaskName)

// Dependency on the distribution task
tasks.getByName("${x86Windows}DistZip")
  .dependsOn(distDep)
  .dependsOn(createJavaRuntimeX86WindowsTaskName)

tasks.getByName("${x64Windows}DistZip")
  .dependsOn(distDep)
  .dependsOn(createJavaRuntimeX64WindowsTaskName)

tasks.getByName(x64LinuxDistZipTaskName)
  .dependsOn(distDep)
  .dependsOn(createJavaRuntimeX64LinuxTaskName)

tasks.getByName("${x64Macos}DistZip")
  .dependsOn(distDep)
  .dependsOn(createJavaRuntimeX64MacosTaskName)
