//https://mvnrepository.com/artifact/software.amazon.awssdk/s3
val awsSdkVersion = "2.20.150"
dependencies {
  api(project(":bytle-type"))
  api(project(":bytle-vertx"))
  implementation("software.amazon.awssdk:s3:$awsSdkVersion")
}
