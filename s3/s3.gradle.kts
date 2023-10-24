//https://mvnrepository.com/artifact/software.amazon.awssdk/s3
val awsSdkVersion = "2.20.150"
val vertxVersion = rootProject.ext.get("vertxVersion").toString()
dependencies {
  api(project(":bytle-type"))
  api(project(":bytle-vertx"))
  implementation("software.amazon.awssdk:s3:$awsSdkVersion")
  testImplementation("io.vertx:vertx-junit5:$vertxVersion")
  testFixturesApi(project(":bytle-vertx"))

}
