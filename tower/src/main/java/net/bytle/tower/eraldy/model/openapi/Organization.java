package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Organization for the users using the Combostrap product (Not from the user of other Realm)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Organization   {

  private Long id;
  private String guid;
  private String name;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public Organization () {
  }

  /**
  * @return id The organization id in the database
  */
  @JsonProperty("id")
  public Long getLocalId() {
    return id;
  }

  /**
  * @param id The organization id in the database
  */
  @SuppressWarnings("unused")
  public void setId(Long id) {
    this.id = id;
  }

  /**
  * @return guid The string representation of the organization id
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The string representation of the organization id
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return name Organization name
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name Organization name
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Organization organization = (Organization) o;
    return Objects.equals(id, organization.id) &&
        Objects.equals(guid, organization.guid) &&
        Objects.equals(name, organization.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, guid, name);
  }

  @Override
  public String toString() {
    return "class Organization {\n" +
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
