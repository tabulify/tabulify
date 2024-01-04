package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A registration
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListRegistration   {


  protected String guid;

  protected Integer status;

  protected ListItem list;

  protected User subscriber;

  protected LocalDateTime confirmationTime;

  protected String confirmationIp;

  protected LocalDateTime optInTime;

  protected String optInIp;

  protected String optInUri;

  protected RegistrationFlow flow;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListRegistration () {
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
  * @return status The status code of the registration (0: registred or subscribed, other: not registred or unsubscribed)
  */
  @JsonProperty("status")
  public Integer getStatus() {
    return status;
  }

  /**
  * @param status The status code of the registration (0: registred or subscribed, other: not registred or unsubscribed)
  */
  @SuppressWarnings("unused")
  public void setStatus(Integer status) {
    this.status = status;
  }

  /**
  * @return list
  */
  @JsonProperty("list")
  public ListItem getList() {
    return list;
  }

  /**
  * @param list Set list
  */
  @SuppressWarnings("unused")
  public void setList(ListItem list) {
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
  public LocalDateTime getOptInTime() {
    return optInTime;
  }

  /**
  * @param optInTime The opt-in time
  */
  @SuppressWarnings("unused")
  public void setOptInTime(LocalDateTime optInTime) {
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
  * @return optInUri The opt-in uri (or a text)  The opt-in uri is used in the mailing reason: You are receiving this email because you subscribe to it from {optInUri} at {optInTime} with the Ip {optInIp}
  */
  @JsonProperty("optInUri")
  public String getOptInUri() {
    return optInUri;
  }

  /**
  * @param optInUri The opt-in uri (or a text)  The opt-in uri is used in the mailing reason: You are receiving this email because you subscribe to it from {optInUri} at {optInTime} with the Ip {optInIp}
  */
  @SuppressWarnings("unused")
  public void setOptInUri(String optInUri) {
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
    ListRegistration listRegistration = (ListRegistration) o;
    return

            Objects.equals(guid, listRegistration.guid) && Objects.equals(status, listRegistration.status) && Objects.equals(list, listRegistration.list) && Objects.equals(subscriber, listRegistration.subscriber) && Objects.equals(confirmationTime, listRegistration.confirmationTime) && Objects.equals(confirmationIp, listRegistration.confirmationIp) && Objects.equals(optInTime, listRegistration.optInTime) && Objects.equals(optInIp, listRegistration.optInIp) && Objects.equals(optInUri, listRegistration.optInUri) && Objects.equals(flow, listRegistration.flow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, status, list, subscriber, confirmationTime, confirmationIp, optInTime, optInIp, optInUri, flow);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
