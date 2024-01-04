package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistrationShort   {


  protected String guid;

  protected String subscriberGuid;

  protected String subscriberEmail;

  protected LocalDateTime confirmationTime;

  protected Integer status;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public RegistrationShort () {
  }

  /**
  * @return guid The public registration id
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The public registration id
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return subscriberGuid The subscriber guid
  */
  @JsonProperty("subscriberGuid")
  public String getSubscriberGuid() {
    return subscriberGuid;
  }

  /**
  * @param subscriberGuid The subscriber guid
  */
  @SuppressWarnings("unused")
  public void setSubscriberGuid(String subscriberGuid) {
    this.subscriberGuid = subscriberGuid;
  }

  /**
  * @return subscriberEmail The subscriber email
  */
  @JsonProperty("subscriberEmail")
  public String getSubscriberEmail() {
    return subscriberEmail;
  }

  /**
  * @param subscriberEmail The subscriber email
  */
  @SuppressWarnings("unused")
  public void setSubscriberEmail(String subscriberEmail) {
    this.subscriberEmail = subscriberEmail;
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
    RegistrationShort registrationShort = (RegistrationShort) o;
    return

            Objects.equals(guid, registrationShort.guid) && Objects.equals(subscriberGuid, registrationShort.subscriberGuid) && Objects.equals(subscriberEmail, registrationShort.subscriberEmail) && Objects.equals(confirmationTime, registrationShort.confirmationTime) && Objects.equals(status, registrationShort.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, subscriberGuid, subscriberEmail, confirmationTime, status);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
