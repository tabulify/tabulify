


description = "Type"

val jacksonVersion = rootProject.ext.get("jacksonVersion").toString()
dependencies {

  api(project(":bytle-log"))
  api(project(":bytle-base"))

  api("com.google.code.gson:gson:2.8.9")
  api("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")

  // should be the same as for Jackson
  api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
  // json path
  api("com.jayway.jsonpath:json-path:2.7.0")
  // databind for serialization of objectMapper that permits to create a JSON tree
  // required by jsonPath when jackson is used https://github.com/json-path/JsonPath#jsonprovider-spi
  api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
  // xml
  api("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
  // stax xml library in used (required by jackson - https://github.com/FasterXML/jackson-dataformat-xml#maven-dependency)
  api("com.fasterxml.woodstox:woodstox-core:6.4.0")
  // html to allow dom selector https://mvnrepository.com/artifact/org.jsoup/jsoup
  api("org.jsoup:jsoup:1.15.3")

  // for testing of hex: import static javax.xml.bind.DatatypeConverter.printHexBinary;
  testImplementation("javax.xml.bind:jaxb-api:2.3.1")

}
