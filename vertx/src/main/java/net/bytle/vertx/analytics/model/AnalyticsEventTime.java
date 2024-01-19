package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * The times of the status
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEventTime   {


  protected LocalDateTime creationTime;

  protected LocalDateTime receptionTime;

  protected LocalDateTime sendingTime;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsEventTime () {
  }

  /**
  * @return creationTime The timestamp when the event has happend (ie when the object was created)
  */
  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  /**
  * @param creationTime The timestamp when the event has happend (ie when the object was created)
  */
  @SuppressWarnings("unused")
  public void setCreationTime(LocalDateTime creationTime) {
    this.creationTime = creationTime;
  }

  /**
  * @return receptionTime The timestamp of when a message was received on the API (if created and send by a client)
  */
  @JsonProperty("receptionTime")
  public LocalDateTime getReceptionTime() {
    return receptionTime;
  }

  /**
  * @param receptionTime The timestamp of when a message was received on the API (if created and send by a client)
  */
  @SuppressWarnings("unused")
  public void setReceptionTime(LocalDateTime receptionTime) {
    this.receptionTime = receptionTime;
  }

  /**
  * @return sendingTime The timestamp of when a message was sent to the analytics endpoint api Time: With Ga, you cannot set the time, It uses the notion of queue https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#qt
  */
  @JsonProperty("sendingTime")
  public LocalDateTime getSendingTime() {
    return sendingTime;
  }

  /**
  * @param sendingTime The timestamp of when a message was sent to the analytics endpoint api Time: With Ga, you cannot set the time, It uses the notion of queue https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#qt
  */
  @SuppressWarnings("unused")
  public void setSendingTime(LocalDateTime sendingTime) {
    this.sendingTime = sendingTime;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsEventTime analyticsEventTime = (AnalyticsEventTime) o;
    return

            Objects.equals(creationTime, analyticsEventTime.creationTime) && Objects.equals(receptionTime, analyticsEventTime.receptionTime) && Objects.equals(sendingTime, analyticsEventTime.sendingTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(creationTime, receptionTime, sendingTime);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
