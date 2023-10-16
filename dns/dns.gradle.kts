
description = "Dns and block list"

val dnsJavaVersion = rootProject.ext.get("dnsJavaVersion").toString()

dependencies {
  api("dnsjava:dnsjava:$dnsJavaVersion")
  implementation(project(":bytle-type"))
  // there is an email in dmarc, we need the type
  // to verify that this an email
  api(project(":bytle-smtp-client"))
}
