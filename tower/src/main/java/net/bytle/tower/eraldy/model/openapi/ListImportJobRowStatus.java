package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * This object represents the status of a row in a import job for a list.
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListImportJobRowStatus   {


  protected String emailAddress;

public enum StatusCodeEnum {
Success(0),
Error(1),
InvalidEmail(2),
ExistingEmail(3);

  private final Integer value;

  StatusCodeEnum(Integer value) {
      this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }
}


  protected int statusCode;

  protected String statusMessage;

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
  public int getStatusCode() {
    return statusCode;
  }

  /**
  * @param statusCode The status code
  */
  @SuppressWarnings("unused")
  public void setStatusCode(int statusCode) {
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

            Objects.equals(emailAddress, listImportJobRowStatus.emailAddress) && Objects.equals(statusCode, listImportJobRowStatus.statusCode) && Objects.equals(statusMessage, listImportJobRowStatus.statusMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(emailAddress, statusCode, statusMessage);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
