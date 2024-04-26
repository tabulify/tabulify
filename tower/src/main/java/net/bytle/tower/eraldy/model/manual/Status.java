package net.bytle.tower.eraldy.model.manual;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The status of an entity
 */
public interface Status {


  /**
   * @return status The code status
   */
  @JsonProperty("code")
  int getCode();

  /**
   * @return status The order
   */
  @JsonProperty("order")
  int getOrder();

  /**
   * @return The status name
   */
  @JsonProperty("name")
  String getName();


  /**
   * @return The status description
   */
  @JsonProperty("description")
  String getDescription();




}
