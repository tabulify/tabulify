description = "Email Client"


val subethaVersion = rootProject.ext.get("subethaVersion").toString()
val simpleEmailVersion = rootProject.ext.get("simpleEmailVersion").toString()
val jakartaEmailVersion = rootProject.ext.get("jakartaEmailVersion").toString()

// Gmail
// See the set of version here https://developers.google.com/gmail/api/quickstart/java
val gmailApiVersion = "v1-rev20220404-2.0.0" // https://mvnrepository.com/artifact/com.google.apis/google-api-services-gmail
val googleClientApiVersion = "2.0.0" // https://mvnrepository.com/artifact/com.google.api-client/google-api-client
val googleClientAuthVersion = "1.34.1"


// A wrapper around simple email
// It integrates DKIM
// and provides Completable future for vertx
// https://github.com/bbottema/simple-java-mail/issues/148

dependencies {

  // the base email java: JavaxMail is now known as Jakarta
  // we use Jakarta below
  // Added by simple email, therefore no version
  // We have replaced javax by jakarta
  // ie javax.mail.MessagingException by jakarta.mail.MessagingException;
  // ...
  // JavaxMail is now known as Jakarta and is generally provided by the framework
  // Added by simple email, therefore no version
  // Api because the library returns Mime, ...Email in the jakarta form
  api("com.sun.mail:jakarta.mail"){
    version {
      strictly(jakartaEmailVersion)
    }
    because("Without version, the mail api is not passed on to project dependent")
  }

  // To parse Mime String Message (core and dom)
  // https://mvnrepository.com/artifact/org.apache.james/apache-mime4j
  api("org.apache.james:apache-mime4j:0.8.9")


  implementation(project(":bytle-type"))
  // for the transactional template
  implementation(project(":bytle-template"))


  // https://mvnrepository.com/artifact/org.simplejavamail/simple-java-mail
  api("org.simplejavamail:simple-java-mail:$simpleEmailVersion")
  api("org.simplejavamail:dkim-module:$simpleEmailVersion")


  implementation ("com.google.api-client:google-api-client:$googleClientApiVersion")
  implementation ("com.google.oauth-client:google-oauth-client-jetty:$googleClientAuthVersion")
  implementation ("com.google.apis:google-api-services-gmail:$gmailApiVersion")

  // gmail
  // testFixtures add them also in Implementation
  testFixturesImplementation ("com.google.api-client:google-api-client:$googleClientApiVersion")
  testFixturesImplementation ("com.google.oauth-client:google-oauth-client-jetty:$googleClientAuthVersion")
  testFixturesImplementation ("com.google.apis:google-api-services-gmail:$gmailApiVersion")


  testImplementation(project(":bytle-db-csv"))

  // For test (wiser) - api to pass it on
  testFixturesApi("com.github.davidmoten:subethasmtp:$subethaVersion")
  testFixturesImplementation(project(":bytle-os"))
  testFixturesImplementation(project(":bytle-type"))
  testFixturesApi("com.sun.mail:jakarta.mail"){
    version {
      strictly(jakartaEmailVersion)
    }
    because("Without version, the mail api is not passed on to project dependent")
  }


}
