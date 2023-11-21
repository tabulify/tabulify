package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistrationShort   {


  protected String guid;

  protected String subscriberEmail;

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

            Objects.equals(guid, registrationShort.guid) && Objects.equals(subscriberEmail, registrationShort.subscriberEmail);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, subscriberEmail);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
