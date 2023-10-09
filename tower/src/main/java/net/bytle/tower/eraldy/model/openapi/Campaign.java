package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Service
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Campaign   {

  private Long id;
  private String guid;
  private String name;
  private String type;
  private Object data;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public Campaign () {
  }

  /**
  * @return id The campaign id
  */
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  /**
  * @param id The campaign id
  */
  @SuppressWarnings("unused")
  public void setId(Long id) {
    this.id = id;
  }

  /**
  * @return guid The campaign id
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The campaign id
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return name The name of the campaign
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name The name of the campaign
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return type The type of the campaign (smtp, ...) that define the data type
  */
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  /**
  * @param type The type of the campaign (smtp, ...) that define the data type
  */
  @SuppressWarnings("unused")
  public void setType(String type) {
    this.type = type;
  }

  /**
  * @return data The configuration for the campaign
  */
  @JsonProperty("data")
  public Object getData() {
    return data;
  }

  /**
  * @param data The configuration for the campaign
  */
  @SuppressWarnings("unused")
  public void setData(Object data) {
    this.data = data;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Campaign campaign = (Campaign) o;
    return Objects.equals(id, campaign.id) &&
        Objects.equals(guid, campaign.guid) &&
        Objects.equals(name, campaign.name) &&
        Objects.equals(type, campaign.type) &&
        Objects.equals(data, campaign.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, guid, name, type, data);
  }

  @Override
  public String toString() {
    return "class Campaign {\n" +
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
