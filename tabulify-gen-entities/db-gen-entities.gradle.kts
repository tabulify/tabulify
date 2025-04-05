

dependencies {
  api(project(":bytle-db"))
  api(project(":bytle-db-flow"))
  api(project(":bytle-db-csv"))
  api(project(":bytle-crypto"))
  api(project(":bytle-db-sqlite"))
  api(project(":bytle-db-jdbc"))
  api(project(":bytle-http"))
  api("com.github.javafaker:javafaker:1.0.2"){
    exclude(group = "org.yaml")
  }
}
description = "Tabulify Entities"


