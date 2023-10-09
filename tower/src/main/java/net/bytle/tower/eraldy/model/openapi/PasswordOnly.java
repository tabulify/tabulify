package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * To update a password
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PasswordOnly   {

  private String password;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public PasswordOnly () {
  }

  /**
  * @return password A password
  */
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }

  /**
  * @param password A password
  */
  @SuppressWarnings("unused")
  public void setPassword(String password) {
    this.password = password;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PasswordOnly passwordOnly = (PasswordOnly) o;
    return Objects.equals(password, passwordOnly.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(password);
  }

  @Override
  public String toString() {
    return "class PasswordOnly {\n" +
    "    password: " + toIndentedString(password) + "\n" +
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
