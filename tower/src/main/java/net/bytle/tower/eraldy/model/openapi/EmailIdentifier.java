package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * An object that identifies an user by its email and realm
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailIdentifier   {


  protected String userEmail;

  protected String realmIdentifier;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public EmailIdentifier () {
  }

  /**
  * @return userEmail An email
  */
  @JsonProperty("userEmail")
  public String getUserEmail() {
    return userEmail;
  }

  /**
  * @param userEmail An email
  */
  @SuppressWarnings("unused")
  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  /**
  * @return realmIdentifier The realm identifier (guid or handle)
  */
  @JsonProperty("realmIdentifier")
  public String getRealmIdentifier() {
    return realmIdentifier;
  }

  /**
  * @param realmIdentifier The realm identifier (guid or handle)
  */
  @SuppressWarnings("unused")
  public void setRealmIdentifier(String realmIdentifier) {
    this.realmIdentifier = realmIdentifier;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmailIdentifier emailIdentifier = (EmailIdentifier) o;
    return
            Objects.equals(userEmail, emailIdentifier.userEmail) && Objects.equals(realmIdentifier, emailIdentifier.realmIdentifier);

  }

  @Override
  public int hashCode() {
    return Objects.hash(userEmail, realmIdentifier);
  }

  @Override
  public String toString() {
    return userEmail + ", " + realmIdentifier;
  }

}
