package net.bytle.tower.eraldy.model.openapi;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import net.bytle.tower.eraldy.model.openapi.ListItem;
import net.bytle.tower.eraldy.model.openapi.ListUserFlow;
import net.bytle.tower.eraldy.model.openapi.User;

/**
 * A user in a list (ie a subscription or a registration)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListUser {


  protected String guid;

  protected Integer status;

  protected ListItem list;

  protected User user;

  protected LocalDateTime confirmationTime;

  protected String confirmationIp;

  protected LocalDateTime optInTime;

  protected String optInIp;

  protected String optInOrigin;

  protected ListUserFlow flow;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListUser() {
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
   * @return user
  */
  @JsonProperty("user")
  public User getUser() {
    return user;
  }

  /**
   * @param user Set user
  */
  @SuppressWarnings("unused")
  public void setUser(User user) {
    this.user = user;
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
  * @return optInOrigin The opt-in origin (a uri or a text)  The opt-in origin is used in the mailing reason: You are receiving this email because you subscribe to it from {optInOrigin} at {optInTime} with the Ip {optInIp}
  */
  @com.fasterxml.jackson.annotation.JsonAlias({"optInUri"})
  @JsonProperty("optInOrigin")
  public String getOptInOrigin() {
    return optInOrigin;
  }

  /**
  * @param optInOrigin The opt-in origin (a uri or a text)  The opt-in origin is used in the mailing reason: You are receiving this email because you subscribe to it from {optInOrigin} at {optInTime} with the Ip {optInIp}
  */
  @SuppressWarnings("unused")
  public void setOptInOrigin(String optInOrigin) {
    this.optInOrigin = optInOrigin;
  }

  /**
  * @return flow
  */
  @JsonProperty("flow")
  public ListUserFlow getFlow() {
    return flow;
  }

  /**
  * @param flow Set flow
  */
  @SuppressWarnings("unused")
  public void setFlow(ListUserFlow flow) {
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
    ListUser listUser = (ListUser) o;
    return

      Objects.equals(guid, listUser.guid) && Objects.equals(status, listUser.status) && Objects.equals(list, listUser.list) && Objects.equals(user, listUser.user) && Objects.equals(confirmationTime, listUser.confirmationTime) && Objects.equals(confirmationIp, listUser.confirmationIp) && Objects.equals(optInTime, listUser.optInTime) && Objects.equals(optInIp, listUser.optInIp) && Objects.equals(optInOrigin, listUser.optInOrigin) && Objects.equals(flow, listUser.flow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, status, list, user, confirmationTime, confirmationIp, optInTime, optInIp, optInOrigin, flow);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
