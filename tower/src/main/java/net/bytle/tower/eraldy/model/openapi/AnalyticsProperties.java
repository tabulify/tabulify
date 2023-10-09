package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsProperties   {

  private String key;
  private String value;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsProperties () {
  }

  /**
  * @return key
  */
  @JsonProperty("key")
  public String getKey() {
    return key;
  }

  /**
  * @param key Set key
  */
  @SuppressWarnings("unused")
  public void setKey(String key) {
    this.key = key;
  }

  /**
  * @return value
  */
  @JsonProperty("value")
  public String getValue() {
    return value;
  }

  /**
  * @param value Set value
  */
  @SuppressWarnings("unused")
  public void setValue(String value) {
    this.value = value;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsProperties analyticsProperties = (AnalyticsProperties) o;
    return Objects.equals(key, analyticsProperties.key) &&
        Objects.equals(value, analyticsProperties.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @Override
  public String toString() {
    return "class AnalyticsProperties {\n" +
    "    key: " + toIndentedString(key) + "\n" +
    "    value: " + toIndentedString(value) + "\n" +
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
