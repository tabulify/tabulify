description = "Crypto"

dependencies {
  implementation(project(":bytle-type"))
  // https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on
  implementation("org.bouncycastle:bcprov-jdk18on:1.75")
  // tink is just here as history
  testImplementation("com.google.crypto.tink:tink:1.6.1")
}
