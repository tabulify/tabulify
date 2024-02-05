package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Objects;

/**
 * The data needed to register a public user to a list
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListUserPostBody   {


  protected String userEmail;

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
  * @return userEmail The email of the public user that want to subscribe
  */
  @JsonProperty("userEmail")
  public String getUserEmail() {
    return userEmail;
  }

  /**
  * @param userEmail The email of the public user that want to subscribe
  */
  @SuppressWarnings("unused")
  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  /**
  * @return redirectUri The redirect Uri where to redirect the browser.  This URI is a template uri and should contains a `:guid` placeholder. This placeholder is replaced with the list user entry guid that can be used to lookup the entry.  The frontend is the driver of the flow, therefore this URI is mandatory.  If another final redirect uri needs to be set, it should be set as a query property of this uri (ie http(s)://redirectUri?redirectUri)
  */
  @JsonProperty("redirectUri")
  public URI getRedirectUri() {
    return redirectUri;
  }

  /**
  * @param redirectUri The redirect Uri where to redirect the browser.  This URI is a template uri and should contains a `:guid` placeholder. This placeholder is replaced with the list user entry guid that can be used to lookup the entry.  The frontend is the driver of the flow, therefore this URI is mandatory.  If another final redirect uri needs to be set, it should be set as a query property of this uri (ie http(s)://redirectUri?redirectUri)
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
            Objects.equals(userEmail, listUserPostBody.userEmail) && Objects.equals(redirectUri, listUserPostBody.redirectUri);

  }

  @Override
  public int hashCode() {
    return Objects.hash(userEmail, redirectUri);
  }

  @Override
  public String toString() {
    return userEmail + ", " + redirectUri.toString();
  }

}
