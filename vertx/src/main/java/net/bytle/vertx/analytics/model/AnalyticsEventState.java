package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * The times of the status (in UTC)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEventState   {


  protected LocalDateTime eventCreationTime;

  protected LocalDateTime eventReceptionTime;

  protected LocalDateTime eventSendingTime;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsEventState () {
  }

  /**
  * @return eventCreationTime The date-time in UTC when the event has happend (ie when the object was created)
  */
  @JsonProperty("eventCreationTime")
  public LocalDateTime getEventCreationTime() {
    return eventCreationTime;
  }

  /**
  * @param eventCreationTime The date-time in UTC when the event has happend (ie when the object was created)
  */
  @SuppressWarnings("unused")
  public void setEventCreationTime(LocalDateTime eventCreationTime) {
    this.eventCreationTime = eventCreationTime;
  }

  /**
  * @return eventReceptionTime The date-time in UTC of when a message was received on the API (if created and send by a client)
  */
  @JsonProperty("eventReceptionTime")
  public LocalDateTime getEventReceptionTime() {
    return eventReceptionTime;
  }

  /**
  * @param eventReceptionTime The date-time in UTC of when a message was received on the API (if created and send by a client)
  */
  @SuppressWarnings("unused")
  public void setEventReceptionTime(LocalDateTime eventReceptionTime) {
    this.eventReceptionTime = eventReceptionTime;
  }

  /**
  * @return eventSendingTime The date-time in UTC of when a message was sent to the analytics endpoint api Time: With Ga, you cannot set the time, It uses the notion of queue https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#qt
  */
  @JsonProperty("eventSendingTime")
  public LocalDateTime getEventSendingTime() {
    return eventSendingTime;
  }

  /**
  * @param eventSendingTime The date-time in UTC of when a message was sent to the analytics endpoint api Time: With Ga, you cannot set the time, It uses the notion of queue https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters#qt
  */
  @SuppressWarnings("unused")
  public void setEventSendingTime(LocalDateTime eventSendingTime) {
    this.eventSendingTime = eventSendingTime;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsEventState analyticsEventState = (AnalyticsEventState) o;
    return

            Objects.equals(eventCreationTime, analyticsEventState.eventCreationTime) && Objects.equals(eventReceptionTime, analyticsEventState.eventReceptionTime) && Objects.equals(eventSendingTime, analyticsEventState.eventSendingTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventCreationTime, eventReceptionTime, eventSendingTime);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
