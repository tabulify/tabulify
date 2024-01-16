package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Objects;

/**
 * The data needed to register a user to a list Note that a user email or a user id is required (OpenAPI cannot describe that in a elegant way) a user email or id is mandatory a list id or guid is mandatory
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListUserPostBody   {


  protected String userEmail;

  protected String listGuid;

  protected URI redirectUri;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListUserPostBody () {
  }

  /**
  * @return userEmail The email of the user that want to subscribe
  */
  @JsonProperty("userEmail")
  public String getUserEmail() {
    return userEmail;
  }

  /**
  * @param userEmail The email of the user that want to subscribe
  */
  @SuppressWarnings("unused")
  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  /**
  * @return listGuid The public list id where the user should be registered
  */
  @JsonProperty("listGuid")
  public String getListGuid() {
    return listGuid;
  }

  /**
  * @param listGuid The public list id where the user should be registered
  */
  @SuppressWarnings("unused")
  public void setListGuid(String listGuid) {
    this.listGuid = listGuid;
  }

  /**
  * @return redirectUri where to redirect the user
  */
  @JsonProperty("redirectUri")
  public URI getRedirectUri() {
    return redirectUri;
  }

  /**
  * @param redirectUri where to redirect the user
  */
  @SuppressWarnings("unused")
  public void setRedirectUri(URI redirectUri) {
    this.redirectUri = redirectUri;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListUserPostBody listUserPostBody = (ListUserPostBody) o;
    return
            Objects.equals(userEmail, listUserPostBody.userEmail) && Objects.equals(listGuid, listUserPostBody.listGuid) && Objects.equals(redirectUri, listUserPostBody.redirectUri);

  }

  @Override
  public int hashCode() {
    return Objects.hash(userEmail, listGuid, redirectUri);
  }

  @Override
  public String toString() {
    return userEmail + ", " + listGuid + ", " + redirectUri.toString();
  }

}
