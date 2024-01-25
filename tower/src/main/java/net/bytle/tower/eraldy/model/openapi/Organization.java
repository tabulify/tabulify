package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Organization for the users using the Combostrap product (Not from the user of other Realm)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Organization   {


  protected String guid;

  protected Long localId;

  protected String handle;

  protected String name;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public Organization () {
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
  * @return localId The organization id in the database
  */
  @com.fasterxml.jackson.annotation.JsonAlias({"id"})
  @JsonProperty("localId")
  public Long getLocalId() {
    return localId;
  }

  /**
  * @param localId The organization id in the database
  */
  @SuppressWarnings("unused")
  public void setLocalId(Long localId) {
    this.localId = localId;
  }

  /**
  * @return handle Organization Handle (a human identifier)
  */
  @JsonProperty("handle")
  public String getHandle() {
    return handle;
  }

  /**
  * @param handle Organization Handle (a human identifier)
  */
  @SuppressWarnings("unused")
  public void setHandle(String handle) {
    this.handle = handle;
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
    return

            Objects.equals(guid, organization.guid) && Objects.equals(localId, organization.localId) && Objects.equals(handle, organization.handle) && Objects.equals(name, organization.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, localId, handle, name);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
