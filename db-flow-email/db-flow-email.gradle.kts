
description = "Db Flow Implementation"


dependencies {

  //implementation(project(":bytle-db"))
  // for the logs
  //implementation(project(":bytle-db-sqlite"))
  //implementation(project(":bytle-type"))
  // for the email flow step
  implementation(project(":bytle-db-flow"))

  implementation(project(":bytle-smtp-client"))

  // Wiser code
  testImplementation(testFixtures(project(":bytle-smtp-client")))
  // The log connection on tabular is based on sqlite by default
  testImplementation(project(":bytle-db-sqlite"))

}
