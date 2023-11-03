package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Realm creation
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RealmPostBody   {


  protected String name;

  protected String handle;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public RealmPostBody () {
  }

  /**
  * @return name A name
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name A name
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return handle A handle (only alphabetical characters)
  */
  @JsonProperty("handle")
  public String getHandle() {
    return handle;
  }

  /**
  * @param handle A handle (only alphabetical characters)
  */
  @SuppressWarnings("unused")
  public void setHandle(String handle) {
    this.handle = handle;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RealmPostBody realmPostBody = (RealmPostBody) o;
    return Objects.equals(name, realmPostBody.name) &&
        Objects.equals(handle, realmPostBody.handle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, handle);
  }

  @Override
  public String toString() {
    return "class RealmPostBody {\n" +

    "    name: " + toIndentedString(name) + "\n" +
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
