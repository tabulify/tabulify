package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Objects;

/**
 * A registration
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Registration   {


  protected String guid;

  protected String status;

  protected RegistrationList list;

  protected User subscriber;

  protected String confirmationTime;

  protected String confirmationIp;

  protected String optInTime;

  protected String optInIp;

  protected URI optInUri;

  protected RegistrationFlow flow;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public Registration () {
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
  * @return status The status of the registration
  */
  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  /**
  * @param status The status of the registration
  */
  @SuppressWarnings("unused")
  public void setStatus(String status) {
    this.status = status;
  }

  /**
  * @return list
  */
  @JsonProperty("list")
  public RegistrationList getList() {
    return list;
  }

  /**
  * @param list Set list
  */
  @SuppressWarnings("unused")
  public void setList(RegistrationList list) {
    this.list = list;
  }

  /**
  * @return subscriber
  */
  @JsonProperty("subscriber")
  public User getSubscriber() {
    return subscriber;
  }

  /**
  * @param subscriber Set subscriber
  */
  @SuppressWarnings("unused")
  public void setSubscriber(User subscriber) {
    this.subscriber = subscriber;
  }

  /**
  * @return confirmationTime The confirmation time
  */
  @JsonProperty("confirmationTime")
  public String getConfirmationTime() {
    return confirmationTime;
  }

  /**
  * @param confirmationTime The confirmation time
  */
  @SuppressWarnings("unused")
  public void setConfirmationTime(String confirmationTime) {
    this.confirmationTime = confirmationTime;
  }

  /**
  * @return confirmationIp The confirmation ip
  */
  @JsonProperty("confirmationIp")
  public String getConfirmationIp() {
    return confirmationIp;
  }

  /**
  * @param confirmationIp The confirmation ip
  */
  @SuppressWarnings("unused")
  public void setConfirmationIp(String confirmationIp) {
    this.confirmationIp = confirmationIp;
  }

  /**
  * @return optInTime The opt-in time
  */
  @JsonProperty("optInTime")
  public String getOptInTime() {
    return optInTime;
  }

  /**
  * @param optInTime The opt-in time
  */
  @SuppressWarnings("unused")
  public void setOptInTime(String optInTime) {
    this.optInTime = optInTime;
  }

  /**
  * @return optInIp The opt-in ip
  */
  @JsonProperty("optInIp")
  public String getOptInIp() {
    return optInIp;
  }

  /**
  * @param optInIp The opt-in ip
  */
  @SuppressWarnings("unused")
  public void setOptInIp(String optInIp) {
    this.optInIp = optInIp;
  }

  /**
  * @return optInUri The opt-in uri
  */
  @JsonProperty("optInUri")
  public URI getOptInUri() {
    return optInUri;
  }

  /**
  * @param optInUri The opt-in uri
  */
  @SuppressWarnings("unused")
  public void setOptInUri(URI optInUri) {
    this.optInUri = optInUri;
  }

  /**
  * @return flow
  */
  @JsonProperty("flow")
  public RegistrationFlow getFlow() {
    return flow;
  }

  /**
  * @param flow Set flow
  */
  @SuppressWarnings("unused")
  public void setFlow(RegistrationFlow flow) {
    this.flow = flow;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Registration registration = (Registration) o;
    return Objects.equals(guid, registration.guid) &&
        Objects.equals(status, registration.status) &&
        Objects.equals(list, registration.list) &&
        Objects.equals(subscriber, registration.subscriber) &&
        Objects.equals(confirmationTime, registration.confirmationTime) &&
        Objects.equals(confirmationIp, registration.confirmationIp) &&
        Objects.equals(optInTime, registration.optInTime) &&
        Objects.equals(optInIp, registration.optInIp) &&
        Objects.equals(optInUri, registration.optInUri) &&
        Objects.equals(flow, registration.flow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, status, list, subscriber, confirmationTime, confirmationIp, optInTime, optInIp, optInUri, flow);
  }

  @Override
  public String toString() {
    return "class Registration {\n" +
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
