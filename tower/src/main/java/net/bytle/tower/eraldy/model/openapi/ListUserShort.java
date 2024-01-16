package net.bytle.tower.eraldy.model.openapi;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListUserShort {


  protected String guid;

  protected String userGuid;

  protected String userEmail;

  protected LocalDateTime confirmationTime;

  protected Integer status;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListUserShort() {
  }

  /**
   * @return guid The list user guid
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
   * @param guid The list user guid
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
   * @return userGuid The user guid
  */
  @JsonProperty("userGuid")
  public String getUserGuid() {
    return userGuid;
  }

  /**
   * @param userGuid The user guid
  */
  @SuppressWarnings("unused")
  public void setUserGuid(String userGuid) {
    this.userGuid = userGuid;
  }

  /**
   * @return userEmail The user email
  */
  @JsonProperty("userEmail")
  public String getUserEmail() {
    return userEmail;
  }

  /**
   * @param userEmail The user email
  */
  @SuppressWarnings("unused")
  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  /**
  * @return confirmationTime The confirmation time
  */
  @JsonProperty("confirmationTime")
  public LocalDateTime getConfirmationTime() {
    return confirmationTime;
  }

  /**
  * @param confirmationTime The confirmation time
  */
  @SuppressWarnings("unused")
  public void setConfirmationTime(LocalDateTime confirmationTime) {
    this.confirmationTime = confirmationTime;
  }

  /**
  * @return status The status of the registration
  */
  @JsonProperty("status")
  public Integer getStatus() {
    return status;
  }

  /**
  * @param status The status of the registration
  */
  @SuppressWarnings("unused")
  public void setStatus(Integer status) {
    this.status = status;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListUserShort listUserShort = (ListUserShort) o;
    return

      Objects.equals(guid, listUserShort.guid) && Objects.equals(userGuid, listUserShort.userGuid) && Objects.equals(userEmail, listUserShort.userEmail) && Objects.equals(confirmationTime, listUserShort.confirmationTime) && Objects.equals(status, listUserShort.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, userGuid, userEmail, confirmationTime, status);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
