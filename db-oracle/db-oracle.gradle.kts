
// ojdbc8 means certified with jdk8
// https://repo1.maven.org/maven2/com/oracle/database/jdbc/ojdbc8/

dependencies {
  implementation(project(":bytle-db-jdbc"))
  implementation("com.oracle.ojdbc:ojdbc8:19.3.0.0") // for the types
  testImplementation(project(":bytle-db-gen"))
  testImplementation(project(":bytle-db-jdbc","test"))
  testImplementation(project(":bytle-test"))
  testImplementation( "org.testcontainers:oracle-xe:1.12.5")
}

description = "Db Oracle"
