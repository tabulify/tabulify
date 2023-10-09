description = "Vertx Smtp"

val vertxVersion = rootProject.ext.get("vertxVersion").toString()
val simpleEmailVersion = rootProject.ext.get("simpleEmailVersion").toString()
dependencies {

  implementation(project(":bytle-vertx"))
  implementation(project(":bytle-type"))
  // Needed for the DNS checks (Spf, block list)
  implementation(project(":bytle-dns"))
  // A client delivery part that can be embedded in a normal server
  implementation(project(":bytle-smtp-client-delivery"))
  // Does not pass a basic test to find a SPF record ...
  // we put it in test only
  // https://mvnrepository.com/artifact/org.apache.james.jspf/apache-jspf-resolver
  testImplementation("org.apache.james.jspf:apache-jspf-resolver:1.0.3"){
    exclude(group = "commons-cli", module = "commons-cli")
    because("We don't use the cli")
  }

  /**
   * To test chunking (ie BDAT command), otherwise we get this error.
   * Suppressed: org.simplejavamail.internal.moduleloader.ModuleLoaderException: Batch module not found,
   * make sure it is on the classpath (https://github.com/bbottema/simple-java-mail/tree/develop/modules/batch-module)
   * 		at app//org.simplejavamail.internal.moduleloader.ModuleLoader.loadModule(ModuleLoader.java:133)
   * 		at app//org.simplejavamail.internal.moduleloader.ModuleLoader.loadBatchModule(ModuleLoader.java:95)
   * 		at app//org.simplejavamail.mailer.internal.MailerImpl.shutdownConnectionPool(MailerImpl.java:395)
   */
  testImplementation("org.simplejavamail:batch-module:$simpleEmailVersion")
  testImplementation("io.vertx:vertx-unit:$vertxVersion")
  testImplementation("io.vertx:vertx-junit5:$vertxVersion")
  testFixturesApi(project(":bytle-vertx"))


}
