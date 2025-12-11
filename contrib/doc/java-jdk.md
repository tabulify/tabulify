# Java Jdk

## JDK
JDK is for now all over the place

### Which version

Choose a LTS (Long-Term Support) so that it gets patch for more than 6 months.
https://api.foojay.io/swagger-ui#/default/getAllMajorVersionsV3

The JDK should be LTS (Long-Term Support)
Disco Api values used to download the jdk https://github.com/foojayio/discoapi

https://api.foojay.io/swagger-ui#/default/getMajorVersionV3

### Maven

We use the https://jreleaser.org/guide/latest/tools/jdks-maven.html
to download the jdk for [Jreleaser](#jreleaser)

### JReleaser

JReleaser needs it for Jlink

For now, the jdk is downloaded via Maven
Set the version in [jdk.version](../../pom.xml)

Download them:

```bash
cd tabulify-cli && mvn jdks:setup-disco -Prelease
# It's coupled to the package goal
cd tabulify-cli && mvn clean package -DskipTests -P release
```

### IDEA

Add it:

* to the project
* to the WSL target

Delete the old one.
If a module uses the old one, you will get in trouble

### SDKMan

`SDK` man in [setup-sdk](../../.envrc.d/setup-sdk.sh) uses the value in Maven

## Compile language level

### Maven Language Level

It's set in the parent.
```xml
<maven.compiler.release>11</maven.compiler.release>
```

The release parameter is better because it also ensures you only use APIs available in that Java version.

Ref: https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-source-and-target.html

### IDEA Language Level

Just synchronize Maven
