package net.bytle.tower.eraldy.graphql.pojo.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.openapi.ListUserSource;
import net.bytle.tower.eraldy.model.openapi.ListUserStatus;

import java.net.InetAddress;
import java.time.LocalDateTime;

/**
 * The props to create or update a list user
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListUserInputProps {


  protected ListUserStatus status;

  protected ListUserSource inListUserSource;

  protected String inOptInOrigin;

  protected LocalDateTime inOptInConfirmationTime;

  protected InetAddress inOptInConfirmationIp;

  protected LocalDateTime inOptInTime;

  protected InetAddress inOptInIp;



  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListUserInputProps() {
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
  * @return inSourceId
  */
  @com.fasterxml.jackson.annotation.JsonAlias({"flow","sourceId"})
  @JsonProperty("inSourceId")
  public ListUserSource getInListUserSource() {
    return inListUserSource;
  }

  /**
  * @param inListUserSource Set inSourceId
  */
  @SuppressWarnings("unused")
  public void setInListUserSource(ListUserSource inListUserSource) {
    this.inListUserSource = inListUserSource;
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



}
