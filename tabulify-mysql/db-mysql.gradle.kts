



dependencies {
  implementation(project(":bytle-db-jdbc"))
  // https://mvnrepository.com/artifact/mysql/mysql-connector-java
  implementation("mysql:mysql-connector-java:8.0.28") // for the types
  testImplementation(project(":bytle-db-gen"))
  testImplementation(project(":bytle-db-jdbc","test"))
  testImplementation(project(":bytle-test"))
  testImplementation(project(":bytle-test"))
  testImplementation("io.github.cdimascio:dotenv-java:3.0.0")
}

description = "Db MySql"
