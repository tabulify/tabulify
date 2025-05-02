package com.tabulify.conf;

public enum ManifestAttribute implements AttributeEnum {


  MANIFEST_VERSION("The version of the manifest", false),
  CONTENT_TYPE("Bundled extensions can use this attribute to find other JAR files containing needed classes", false),
  CLASS_PATH("??", false),
  SIGNATURE_VERSION("??", false),
  MAIN_CLASS("used for launching applications packaged in JAR files with the --jar option", false),
  SEALED("for sealing", false),
  // technotes/guides/extensions/spec.html#dependency
  EXTENSION_LIST("declaring dependencies on installed extensions", false),
  // technotes/guides/extensions/spec.html#dependency
  EXTENSION_NAME("used for declaring dependencies on installed extensions", false),
  // See Java Product Versioning Specification, technotes/guides/versioning/spec/versioning2.html#wp90779
  IMPLEMENTATION_TITLE("manifest attribute used for package versioning", false),
  // manifest attribute used for package versioning.
  IMPLEMENTATION_VERSION("manifest attribute used for package versioning", false),
  // manifest attribute used for package versioning.
  IMPLEMENTATION_VENDOR("manifest attribute used for package versioning", false),
  // manifest attribute used for package versioning.
  SPECIFICATION_TITLE("manifest attribute used for package versioning", false),
  // manifest attribute used for package versioning.
  SPECIFICATION_VERSION("manifest attribute used for package versioning", false),
  // manifest attribute used for package versioning.
  SPECIFICATION_VENDOR("manifest attribute used for package versioning", false),
  // The below parameters are not standard
  DESCRIPTION("The package description", false),
  PACKAGE_TITLE("The name of the package", true),
  PACKAGE_VERSION("The version", true),
  PACKAGE_VENDOR("The package vendor", true),
  BUILD_COMMIT("The build commit", true),
  BUILD_TIME("The build time", true),
  BUILD_JAVA_VERSION("The build java version", true),
  BUILD_GRADLE_VERSION("The build gradle version", true),
  BUILD_OS_VERSION("The build os version", true),
  BUILD_OS_NAME("The os name", true),
  BUILD_OS_ARCH("The os architecture", true);

  private final String description;
  /**
   * Do we print the information on version
   */
  private final Boolean isVersion;

  ManifestAttribute(String description, Boolean isVersionAttribute) {
    this.description = description;
    this.isVersion = isVersionAttribute;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Object getDefaultValue() {
    return "";
  }

  @Override
  public Class<?> getValueClazz() {
    return String.class;
  }

  @SuppressWarnings("unused")
  public Boolean isVersion() {
    return isVersion;
  }


}
