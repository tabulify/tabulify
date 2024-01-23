package net.bytle.vertx.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.vertx.analytics.model.AnalyticsEvent;

import java.util.*;

/**
 * A status object around a {@link net.bytle.vertx.analytics.model.AnalyticsEvent}
 * to manage the delivery
 * It's serialized by Jackson and should therefore contain only data
 * If you had properties, don't forget to update the equals and hash
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEventDeliveryStatus {

  /**
   * The event to deliver
   */
  private AnalyticsEvent analyticsEvent;
  /**
   * A set of sink that should deliver this message
   * The delivery is complete when the set is empty
   */
  private Set<String> sinkNamesToComplete = new HashSet<>();

  /**
   * A count of the fatal error by sink name
   */
  private final Map<String, Integer> sinksFatalErrorCount = new HashMap<>();

  /**
   * The empty constructor is
   * needed for the construction of the pojo
   * with the Jackson library
   */
  @SuppressWarnings("unused")
  public AnalyticsEventDeliveryStatus() {
  }


  public AnalyticsEventDeliveryStatus(AnalyticsEvent analyticsEvent, Set<String> sinksToDeliver) {
    this.analyticsEvent = analyticsEvent;
    this.sinkNamesToComplete = new HashSet<>(sinksToDeliver);
  }

  public boolean isDeliveredForSink(String sinkName) {
    return !sinkNamesToComplete.contains(sinkName);
  }

  public boolean isComplete() {
    if (this.analyticsEvent == null) {
      // May happen in dev when the serialization goes wrong
      return true;
    }
    return sinkNamesToComplete.isEmpty();
  }

  @JsonProperty("analyticsEvent")
  public AnalyticsEvent getAnalyticsEvent() {
    return this.analyticsEvent;
  }

  @JsonProperty("sinksToComplete")
  public Set<String> getSinksToComplete() {
    return this.sinkNamesToComplete;
  }

  @JsonProperty("sinksFatalErrorCount")
  public Map<String, Integer> getSinksFatalErrorCount() {
    return this.sinksFatalErrorCount;
  }

  public Integer incrementFatalErrorCounterForSink(String sinkName) {
    Integer count = this.sinksFatalErrorCount.getOrDefault(sinkName, 0);
    count++;
    this.sinksFatalErrorCount.put(sinkName, count);
    return count;
  }


  public void deliveredForSink(String sinkName) {
    this.sinkNamesToComplete.remove(sinkName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AnalyticsEventDeliveryStatus that = (AnalyticsEventDeliveryStatus) o;
    return Objects.equals(analyticsEvent, that.analyticsEvent) && Objects.equals(sinkNamesToComplete, that.sinkNamesToComplete) && Objects.equals(sinksFatalErrorCount, that.sinksFatalErrorCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(analyticsEvent, sinkNamesToComplete, sinksFatalErrorCount);
  }

}
