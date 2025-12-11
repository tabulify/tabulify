package com.tabulify.java;

import com.tabulify.type.KeyInterface;

/**
 * See
 * <a href="https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html">...</a>
 * And
 * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/versioning/spec/versioning2.html#wp89936">...</a>
 */
public enum JarManifestAttribute implements KeyInterface {


  MANIFEST_VERSION("The version of the manifest"),
  CONTENT_TYPE("Bundled extensions can use this attribute to find other JAR files containing needed classes"),
  CLASS_PATH("Class path"),
  SIGNATURE_VERSION("Signature Version"),
  // used for launching applications packaged in JAR files with the --jar option
  MAIN_CLASS("The main class"),
  SEALED("True if sealed"),
  // technotes/guides/extensions/spec.html#dependency
  EXTENSION_LIST("declaring dependencies on installed extensions"),
  // technotes/guides/extensions/spec.html#dependency
  EXTENSION_NAME("used for declaring dependencies on installed extensions"),
  // The specification fields describe what standard or API your code adheres to, while the implementation fields describe your particular version of that code.
  SPECIFICATION_TITLE("The title of the specification."),
  SPECIFICATION_VERSION("The version of the specification."),
  SPECIFICATION_VENDOR("The vendor of the specification."),
  // The specification fields describe what standard or API your code adheres to, while the implementation fields describe your particular version of that code.
  // See Java Product Versioning Specification, technotes/guides/versioning/spec/versioning2.html#wp90779
  IMPLEMENTATION_TITLE("The title of the implementation."),
  // manifest attribute used for package versioning.
  IMPLEMENTATION_VERSION("The build number of the implementation"),
  // manifest attribute used for package versioning.
  IMPLEMENTATION_VENDOR("The vendor of the implementation."),
  /**
   * Package
   * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/versioning/spec/versioning2.html#wp90779">...</a>
   */
  PACKAGE_TITLE("The package title"),
  PACKAGE_VERSION("The package version"),
  PACKAGE_VENDOR("The package vendor"),
  PACKAGE_VENDOR_URL("The package vendor url"),
  /**
   * Creator, for instance Maven JAR Plugin 3.4.1
   */
  CREATED_BY("The creator"),
  //
  // The below parameters are not standard
  //
  /**
   * Maven
   */
  MAVEN_PROJECT_GROUP_ID("The maven project group id (The organization)"),
  MAVEN_PROJECT_ARTIFACT_ID("The maven project artifact id (The project name)"),
  MAVEN_PROJECT_VERSION("The maven project version (The version)"),
  /**
   * OS
   */
  OS_VERSION("The build os version"),
  OS_NAME("The os name"),
  OS_ARCH("The os architecture"),
  /**
   * Java
   */
  JAVA_VERSION("The java version"),
  JAVA_VERSION_DATE("The java version date"),
  JAVA_VENDOR("The java vendor"),
  JAVA_VENDOR_URL("The java vendor url"),
  JAVA_VENDOR_VERSION("The java vendor version"),
  JAVA_VM_NAME("The java vm name"),
  JAVA_VM_VERSION("The java vm version"),
  JAVA_VM_VENDOR("The java vm vendor"),
  JAVA_VM_INFO("The java vm info"),
  JAVA_RUNTIME_NAME("The java runtime name"),
  JAVA_RUNTIME_VERSION("The java runtime version"),
  /**
   * Build tool version
   */
  GRADLE_VERSION("The gradle version"),
  MAVEN_VERSION("The maven version"),
  MAVEN_BUILD_VERSION("The maven build version"),
  // https://maven.apache.org/plugins/maven-resources-plugin/apidocs/org/apache/maven/plugins/resources/MavenBuildTimestamp.html
  MAVEN_BUILD_TIMESTAMP("The maven build start timestamp (at UTC)"),
  /**
   * Build
   */
  BUILD_HOST("The build host"),
  // On gitHub, gitLab, ...
  BUILD_NUMBER("The Continuous Integration build id"),
  // Added by the maven jar plugin
  BUILD_JDK_SPEC("The Build JDK specification"),
  /**
   * GIT
   */
  GIT_BRANCH("The git branch"),
  GIT_DIRTY("True if Git was dirty (Uncommited changes were present)"),
  GIT_COMMIT("The commit"),
  GIT_COMMIT_TIME("The commit time"),
  GIT_COMMIT_ABBREV("The short commit hash"),
  GIT_COMMIT_USER_NAME("The commit user name"),
  GIT_COMMIT_USER_EMAIL("The commit user email"),
  GIT_COMMIT_MESSAGE_SHORT("The short commit message"),
  GIT_COMMIT_MESSAGE_FULL("The git commit message"),
  GIT_COMMIT_DESCRIBE("The git human readable name"),
  GIT_COMMIT_AUTHOR_TIME("The timestamp when the commit has been originally performed"),
  GIT_COMMIT_COMMITTER_TIME("The timestamp when the commit has been performed"),
  /**
   * Same as git tag --points-at HEAD
   */
  GIT_TAG("The tag"),
  /**
   * git tag --contains
   */
  GIT_TAGS("The tags that contains the commits"),

  ;

  private final String description;

  JarManifestAttribute(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }


}
