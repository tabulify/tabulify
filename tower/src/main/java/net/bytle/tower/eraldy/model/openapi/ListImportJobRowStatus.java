package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.api.implementer.flow.ListImportListUserStatus;
import net.bytle.tower.eraldy.api.implementer.flow.ListImportUserStatus;
import net.bytle.tower.eraldy.module.list.model.ListUserGuid;
import net.bytle.tower.eraldy.module.user.model.UserGuid;

import java.util.Objects;

/**
 * This object represents the status of a row in a import job for a list.
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListImportJobRowStatus   {


  protected String emailAddress;

  protected Integer statusCode;

  protected String statusMessage;

  protected Integer rowId;

  protected UserGuid userGuid;

  protected ListImportUserStatus userStatus;

  protected ListUserGuid listUserGuid;

  protected ListImportListUserStatus listUserStatus;

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
  * @return rowId The row id in the input file
  */
  @JsonProperty("rowId")
  public Integer getRowId() {
    return rowId;
  }

  /**
  * @param rowId The row id in the input file
  */
  @SuppressWarnings("unused")
  public void setRowId(Integer rowId) {
    this.rowId = rowId;
  }

  /**
  * @return userGuid The user guid (may be null)
  */
  @JsonProperty("userGuid")
  public UserGuid getUserGuid() {
    return userGuid;
  }

  /**
  * @param userGuid The user guid (may be null)
  */
  @SuppressWarnings("unused")
  public void setUserGuid(UserGuid userGuid) {
    this.userGuid = userGuid;
  }

  /**
  * @return userStatus The status code of the user (0 - nothing done, 1 - user updated, 2 - user created)
  */
  @JsonProperty("userStatus")
  public ListImportUserStatus getUserStatus() {
    return userStatus;
  }

  /**
  * @param userStatus The status code of the user (0 - nothing done, 1 - user updated, 2 - user created)
  */
  @SuppressWarnings("unused")
  public void setImportUserStatus(ListImportUserStatus userStatus) {
    this.userStatus = userStatus;
  }

  /**
  * @return listUserGuid The list user guid (may be null if there is an error)
  */
  @JsonProperty("listUserGuid")
  public ListUserGuid getListUserGuid() {
    return listUserGuid;
  }

  /**
  * @param listUserGuid The list user guid (may be null if there is an error)
  */
  @SuppressWarnings("unused")
  public void setListUserGuid(ListUserGuid listUserGuid) {
    this.listUserGuid = listUserGuid;
  }

  /**
  * @return listUserStatus The status code of the user in the list (0 - nothing done, 1 - user added)
  */
  @JsonProperty("listUserStatus")
  public ListImportListUserStatus getListUserStatus() {
    return listUserStatus;
  }

  /**
  * @param listUserStatus The status code of the user in the list (0 - nothing done, 1 - user added)
  */
  @SuppressWarnings("unused")
  public void setImportListUserStatus(ListImportListUserStatus listUserStatus) {
    this.listUserStatus = listUserStatus;
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

            Objects.equals(emailAddress, listImportJobRowStatus.emailAddress) && Objects.equals(statusCode, listImportJobRowStatus.statusCode) && Objects.equals(statusMessage, listImportJobRowStatus.statusMessage) && Objects.equals(rowId, listImportJobRowStatus.rowId) && Objects.equals(userGuid, listImportJobRowStatus.userGuid) && Objects.equals(userStatus, listImportJobRowStatus.userStatus) && Objects.equals(listUserGuid, listImportJobRowStatus.listUserGuid) && Objects.equals(listUserStatus, listImportJobRowStatus.listUserStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(emailAddress, statusCode, statusMessage, rowId, userGuid, userStatus, listUserGuid, listUserStatus);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
