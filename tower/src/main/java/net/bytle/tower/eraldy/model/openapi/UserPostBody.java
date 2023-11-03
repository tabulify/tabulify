package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Objects;

/**
 * A user. If the guid is given, it&#39;s used to identify the user otherwise, it&#39;s the email
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPostBody   {


  protected String userGuid;

  protected String realmGuid;

  protected String realmHandle;

  protected String userEmail;

  protected String userName;

  protected String userFullname;

  protected String userTitle;

  protected URI userAvatar;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public UserPostBody () {
  }

  /**
  * @return userGuid The global/string user id
  */
  @JsonProperty("userGuid")
  public String getUserGuid() {
    return userGuid;
  }

  /**
  * @param userGuid The global/string user id
  */
  @SuppressWarnings("unused")
  public void setUserGuid(String userGuid) {
    this.userGuid = userGuid;
  }

  /**
  * @return realmGuid The realm Guid
  */
  @JsonProperty("realmGuid")
  public String getRealmGuid() {
    return realmGuid;
  }

  /**
  * @param realmGuid The realm Guid
  */
  @SuppressWarnings("unused")
  public void setRealmGuid(String realmGuid) {
    this.realmGuid = realmGuid;
  }

  /**
  * @return realmHandle The realm Handle
  */
  @JsonProperty("realmHandle")
  public String getRealmHandle() {
    return realmHandle;
  }

  /**
  * @param realmHandle The realm Handle
  */
  @SuppressWarnings("unused")
  public void setRealmHandle(String realmHandle) {
    this.realmHandle = realmHandle;
  }

  /**
  * @return userEmail The email of the user
  */
  @JsonProperty("userEmail")
  public String getUserEmail() {
    return userEmail;
  }

  /**
  * @param userEmail The email of the user
  */
  @SuppressWarnings("unused")
  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  /**
  * @return userName The short and informal name of the user (used in signature)
  */
  @JsonProperty("userName")
  public String getUserName() {
    return userName;
  }

  /**
  * @param userName The short and informal name of the user (used in signature)
  */
  @SuppressWarnings("unused")
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
  * @return userFullname The long and formal name of the user (used in address)
  */
  @JsonProperty("userFullname")
  public String getUserFullname() {
    return userFullname;
  }

  /**
  * @param userFullname The long and formal name of the user (used in address)
  */
  @SuppressWarnings("unused")
  public void setUserFullname(String userFullname) {
    this.userFullname = userFullname;
  }

  /**
  * @return userTitle The title of the user (A short description)
  */
  @JsonProperty("userTitle")
  public String getUserTitle() {
    return userTitle;
  }

  /**
  * @param userTitle The title of the user (A short description)
  */
  @SuppressWarnings("unused")
  public void setUserTitle(String userTitle) {
    this.userTitle = userTitle;
  }

  /**
  * @return userAvatar An image avatar of a user
  */
  @JsonProperty("userAvatar")
  public URI getUserAvatar() {
    return userAvatar;
  }

  /**
  * @param userAvatar An image avatar of a user
  */
  @SuppressWarnings("unused")
  public void setUserAvatar(URI userAvatar) {
    this.userAvatar = userAvatar;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserPostBody userPostBody = (UserPostBody) o;
    return Objects.equals(userGuid, userPostBody.userGuid) &&
        Objects.equals(realmGuid, userPostBody.realmGuid) &&
        Objects.equals(realmHandle, userPostBody.realmHandle) &&
        Objects.equals(userEmail, userPostBody.userEmail) &&
        Objects.equals(userName, userPostBody.userName) &&
        Objects.equals(userFullname, userPostBody.userFullname) &&
        Objects.equals(userTitle, userPostBody.userTitle) &&
        Objects.equals(userAvatar, userPostBody.userAvatar);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userGuid, realmGuid, realmHandle, userEmail, userName, userFullname, userTitle, userAvatar);
  }

  @Override
  public String toString() {
    return "class UserPostBody {\n" +
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
