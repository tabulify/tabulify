package net.bytle.tower.eraldy.model.openapi;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
  * @return handle A handle (follow the same constraint that domain name)
  */
  @JsonProperty("handle")
  public String getHandle() {
    return handle;
  }

  /**
  * @param handle A handle (follow the same constraint that domain name)
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
    return
            Objects.equals(name, realmPostBody.name);

  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return name;
  }

}
