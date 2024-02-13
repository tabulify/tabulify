
description = "Dns and block list"

val dnsJavaVersion = rootProject.ext.get("dnsJavaVersion").toString()

dependencies {
  api("dnsjava:dnsjava:$dnsJavaVersion")
  implementation(project(":bytle-type"))
}
