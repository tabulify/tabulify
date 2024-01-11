package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * This object represents the status of a row in a import job for a list.
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListImportJobRowStatus   {


  protected String emailAddress;

  protected String userGuid;

  protected Integer statusCode;

  protected String statusMessage;

  protected Boolean userAdded;

  protected Boolean userCreated;

  protected Boolean userUpdated;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListImportJobRowStatus () {
  }

  /**
  * @return emailAddress The email to import
  */
  @JsonProperty("emailAddress")
  public String getEmailAddress() {
    return emailAddress;
  }

  /**
  * @param emailAddress The email to import
  */
  @SuppressWarnings("unused")
  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  /**
  * @return userGuid The user guid (may be null)
  */
  @JsonProperty("userGuid")
  public String getUserGuid() {
    return userGuid;
  }

  /**
  * @param userGuid The user guid (may be null)
  */
  @SuppressWarnings("unused")
  public void setUserGuid(String userGuid) {
    this.userGuid = userGuid;
  }

  /**
  * @return statusCode The status code
  */
  @JsonProperty("statusCode")
  public Integer getStatusCode() {
    return statusCode;
  }

  /**
  * @param statusCode The status code
  */
  @SuppressWarnings("unused")
  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  /**
  * @return statusMessage A message
  */
  @JsonProperty("statusMessage")
  public String getStatusMessage() {
    return statusMessage;
  }

  /**
  * @param statusMessage A message
  */
  @SuppressWarnings("unused")
  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  /**
  * @return userAdded True if the user was added to the list
  */
  @JsonProperty("userAdded")
  public Boolean getUserAdded() {
    return userAdded;
  }

  /**
  * @param userAdded True if the user was added to the list
  */
  @SuppressWarnings("unused")
  public void setUserAdded(Boolean userAdded) {
    this.userAdded = userAdded;
  }

  /**
  * @return userCreated True if the user was created in the realm
  */
  @JsonProperty("userCreated")
  public Boolean getUserCreated() {
    return userCreated;
  }

  /**
  * @param userCreated True if the user was created in the realm
  */
  @SuppressWarnings("unused")
  public void setUserCreated(Boolean userCreated) {
    this.userCreated = userCreated;
  }

  /**
  * @return userUpdated True if the user profile was updated in the realm
  */
  @JsonProperty("userUpdated")
  public Boolean getUserUpdated() {
    return userUpdated;
  }

  /**
  * @param userUpdated True if the user profile was updated in the realm
  */
  @SuppressWarnings("unused")
  public void setUserUpdated(Boolean userUpdated) {
    this.userUpdated = userUpdated;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListImportJobRowStatus listImportJobRowStatus = (ListImportJobRowStatus) o;
    return

            Objects.equals(emailAddress, listImportJobRowStatus.emailAddress) && Objects.equals(userGuid, listImportJobRowStatus.userGuid) && Objects.equals(statusCode, listImportJobRowStatus.statusCode) && Objects.equals(statusMessage, listImportJobRowStatus.statusMessage) && Objects.equals(userAdded, listImportJobRowStatus.userAdded) && Objects.equals(userCreated, listImportJobRowStatus.userCreated) && Objects.equals(userUpdated, listImportJobRowStatus.userUpdated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(emailAddress, userGuid, statusCode, statusMessage, userAdded, userCreated, userUpdated);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
