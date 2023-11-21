package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A auth post object based on email to register, get a magic link, ...
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthEmailPost   {


  protected String userEmail;

  protected String realmIdentifier;

  protected String redirectUri;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AuthEmailPost () {
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

  /**
  * @return redirectUri The uri where to redirect the user after successful registration
  */
  @JsonProperty("redirectUri")
  public String getRedirectUri() {
    return redirectUri;
  }

  /**
  * @param redirectUri The uri where to redirect the user after successful registration
  */
  @SuppressWarnings("unused")
  public void setRedirectUri(String redirectUri) {
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
    AuthEmailPost authEmailPost = (AuthEmailPost) o;
    return
            Objects.equals(userEmail, authEmailPost.userEmail) && Objects.equals(realmIdentifier, authEmailPost.realmIdentifier) && Objects.equals(redirectUri, authEmailPost.redirectUri);

  }

  @Override
  public int hashCode() {
    return Objects.hash(userEmail, realmIdentifier, redirectUri);
  }

  @Override
  public String toString() {
    return userEmail + ", " + realmIdentifier + ", " + redirectUri;
  }

}
