package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A post mailing object to create a mailing
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListMailingCreationPost   {


  protected String name;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListMailingCreationPost () {
  }

  /**
  * @return name The name of the mailing
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name The name of the mailing
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
    ListMailingCreationPost listMailingCreationPost = (ListMailingCreationPost) o;
    return

            Objects.equals(name, listMailingCreationPost.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
