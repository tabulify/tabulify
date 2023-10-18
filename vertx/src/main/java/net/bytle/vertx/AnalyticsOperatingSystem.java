package net.bytle.vertx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * The os data
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsOperatingSystem   {

  private String name;
  private String version;
  private String arch;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsOperatingSystem () {
  }

  /**
  * @return name the name
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name the name
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return version the version
  */
  @JsonProperty("version")
  public String getVersion() {
    return version;
  }

  /**
  * @param version the version
  */
  @SuppressWarnings("unused")
  public void setVersion(String version) {
    this.version = version;
  }

  /**
  * @return arch the architecture
  */
  @JsonProperty("arch")
  public String getArch() {
    return arch;
  }

  /**
  * @param arch the architecture
  */
  @SuppressWarnings("unused")
  public void setArch(String arch) {
    this.arch = arch;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsOperatingSystem analyticsOperatingSystem = (AnalyticsOperatingSystem) o;
    return Objects.equals(name, analyticsOperatingSystem.name) &&
        Objects.equals(version, analyticsOperatingSystem.version) &&
        Objects.equals(arch, analyticsOperatingSystem.arch);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version, arch);
  }

  @Override
  public String toString() {
    return "class AnalyticsOperatingSystem {\n" +
    "}";
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
