package net.bytle.vertx.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A status object to manage the delivery
 * In the Future, they may be serialized by Jackson and should therefore contain only data
 */
public class AnalyticsDeliveryStatus<T> {

  /**
   * The object to deliver
   */
  private final T deliveryObject;
  /**
   * A set of sink that should deliver this message
   * The delivery is complete when the set is empty
   */
  private final Set<String> sinkNamesToComplete;

  /**
   * A count of the fatal error by sink name
   */
  private final Map<String, Integer> sinksFatalErrorCount = new HashMap<>();



  public AnalyticsDeliveryStatus(T deliveryObject, Set<String> sinksToDeliver) {
    this.deliveryObject = deliveryObject;
    this.sinkNamesToComplete = new HashSet<>(sinksToDeliver);
  }

  public boolean isDeliveredForSink(String sinkName) {
    return !sinkNamesToComplete.contains(sinkName);
  }

  public boolean isComplete() {
    if (this.deliveryObject == null) {
      // May happen in dev when the serialization goes wrong
      return true;
    }
    return sinkNamesToComplete.isEmpty();
  }

  @JsonProperty("deliveryObject")
  public T getDeliveryObject() {
    return this.deliveryObject;
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



}
