package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.list.jackson.JacksonListUserSourceDeserializer;
import net.bytle.tower.eraldy.module.list.model.ListUserGuid;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A user in a list (ie a subscription or a registration)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListUser   {


  protected ListUserGuid guid;

  protected ListUserStatus status;

  protected ListObject list;

  protected User user;

  protected ListUserSource inSourceId;

  protected String inOptInOrigin;

  protected LocalDateTime inOptInConfirmationTime;

  protected InetAddress inOptInConfirmationIp;

  protected LocalDateTime inOptInTime;

  protected InetAddress inOptInIp;

  protected LocalDateTime outOptOutTime;

  protected LocalDateTime creationTime;

  protected LocalDateTime modificationTime;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListUser () {
  }

  /**
  * @return guid The listing guid
  */
  @JsonProperty("guid")
  public ListUserGuid getGuid() {
    return guid;
  }

  /**
  * @param guid The listing guid
  */
  @SuppressWarnings("unused")
  public void setGuid(ListUserGuid guid) {
    this.guid = guid;
  }

  /**
  * @return status
  */
  @JsonProperty("status")
  public ListUserStatus getStatus() {
    return status;
  }

  /**
  * @param status Set status
  */
  @SuppressWarnings("unused")
  public void setStatus(ListUserStatus status) {
    this.status = status;
  }

  /**
  * @return list
  */
  @JsonProperty("list")
  public ListObject getList() {
    return list;
  }

  /**
  * @param list Set list
  */
  @SuppressWarnings("unused")
  public void setList(ListObject list) {
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
  * @return inSourceId
  */
  @com.fasterxml.jackson.annotation.JsonAlias({"flow","sourceId"}) @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = JacksonListUserSourceDeserializer.class)
  @JsonProperty("inSourceId")
  public ListUserSource getInSourceId() {
    return inSourceId;
  }

  /**
  * @param inSourceId Set inSourceId
  */
  @SuppressWarnings("unused")
  public void setInSourceId(ListUserSource inSourceId) {
    this.inSourceId = inSourceId;
  }

  /**
  * @return inOptInOrigin The listing origin (a uri or a text)  The listing origin is used in the mailing reason: You are receiving this email because you subscribe to it from {origin} at {optInTime} with the Ip {optInIp}
  */
  @com.fasterxml.jackson.annotation.JsonAlias({"optInUri","optInOrigin"})
  @JsonProperty("inOptInOrigin")
  public String getInOptInOrigin() {
    return inOptInOrigin;
  }

  /**
  * @param inOptInOrigin The listing origin (a uri or a text)  The listing origin is used in the mailing reason: You are receiving this email because you subscribe to it from {origin} at {optInTime} with the Ip {optInIp}
  */
  @SuppressWarnings("unused")
  public void setInOptInOrigin(String inOptInOrigin) {
    this.inOptInOrigin = inOptInOrigin;
  }

  /**
  * @return inOptInConfirmationTime The confirmation time
  */
  @com.fasterxml.jackson.annotation.JsonAlias({"confirmationTime"})
  @JsonProperty("inOptInConfirmationTime")
  public LocalDateTime getInOptInConfirmationTime() {
    return inOptInConfirmationTime;
  }

  /**
  * @param inOptInConfirmationTime The confirmation time
  */
  @SuppressWarnings("unused")
  public void setInOptInConfirmationTime(LocalDateTime inOptInConfirmationTime) {
    this.inOptInConfirmationTime = inOptInConfirmationTime;
  }

  /**
  * @return inOptInConfirmationIp The confirmation ip
  */
  @com.fasterxml.jackson.annotation.JsonAlias({"confirmationIp"})
  @JsonProperty("inOptInConfirmationIp")
  public InetAddress getInOptInConfirmationIp() {
    return inOptInConfirmationIp;
  }

  /**
  * @param inOptInConfirmationIp The confirmation ip
  */
  @SuppressWarnings("unused")
  public void setInOptInConfirmationIp(InetAddress inOptInConfirmationIp) {
    this.inOptInConfirmationIp = inOptInConfirmationIp;
  }

  /**
  * @return inOptInTime The opt-in time
  */
  @com.fasterxml.jackson.annotation.JsonAlias({"optInTime"})
  @JsonProperty("inOptInTime")
  public LocalDateTime getInOptInTime() {
    return inOptInTime;
  }

  /**
  * @param inOptInTime The opt-in time
  */
  @SuppressWarnings("unused")
  public void setInOptInTime(LocalDateTime inOptInTime) {
    this.inOptInTime = inOptInTime;
  }

  /**
  * @return inOptInIp The opt-in ip
  */
  @com.fasterxml.jackson.annotation.JsonAlias({"optInIp"})
  @JsonProperty("inOptInIp")
  public InetAddress getInOptInIp() {
    return inOptInIp;
  }

  /**
  * @param inOptInIp The opt-in ip
  */
  @SuppressWarnings("unused")
  public void setInOptInIp(InetAddress inOptInIp) {
    this.inOptInIp = inOptInIp;
  }

  /**
  * @return outOptOutTime The opt-out time
  */
  @JsonProperty("outOptOutTime")
  public LocalDateTime getOutOptOutTime() {
    return outOptOutTime;
  }

  /**
  * @param outOptOutTime The opt-out time
  */
  @SuppressWarnings("unused")
  public void setOutOptOutTime(LocalDateTime outOptOutTime) {
    this.outOptOutTime = outOptOutTime;
  }

  /**
  * @return creationTime The creation time of the listing
  */
  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  /**
  * @param creationTime The creation time of the listing
  */
  @SuppressWarnings("unused")
  public void setCreationTime(LocalDateTime creationTime) {
    this.creationTime = creationTime;
  }

  /**
  * @return modificationTime The last modification time of the listing
  */
  @JsonProperty("modificationTime")
  public LocalDateTime getModificationTime() {
    return modificationTime;
  }

  /**
  * @param modificationTime The last modification time of the listing
  */
  @SuppressWarnings("unused")
  public void setModificationTime(LocalDateTime modificationTime) {
    this.modificationTime = modificationTime;
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
            Objects.equals(guid, listUser.guid);

  }

  @Override
  public int hashCode() {
    return Objects.hash(guid);
  }

  @Override
  public String toString() {
    return guid.toString();
  }

}
