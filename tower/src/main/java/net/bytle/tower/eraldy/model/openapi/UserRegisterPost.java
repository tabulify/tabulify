package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A post object to register
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRegisterPost   {


  protected String userEmail;

  protected String realmIdentifier;

  protected String redirectUri;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public UserRegisterPost () {
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
    UserRegisterPost userRegisterPost = (UserRegisterPost) o;
    return Objects.equals(userEmail, userRegisterPost.userEmail) &&
        Objects.equals(realmIdentifier, userRegisterPost.realmIdentifier) &&
        Objects.equals(redirectUri, userRegisterPost.redirectUri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userEmail, realmIdentifier, redirectUri);
  }

  @Override
  public String toString() {
    return "class UserRegisterPost {\n" +

    "    userEmail: " + toIndentedString(userEmail) + "\n" +

    "    realmIdentifier: " + toIndentedString(realmIdentifier) + "\n" +

    "    redirectUri: " + toIndentedString(redirectUri) + "\n" +
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
