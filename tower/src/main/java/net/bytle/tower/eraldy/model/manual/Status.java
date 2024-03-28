package net.bytle.tower.eraldy.model.manual;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The status of an entity
 */
public class Status {

  private Integer code;
  private String name;

  /**
   * The empty constructor is
   * needed for the construction of the pojo
   * with the Jackson library
   */
  @SuppressWarnings("unused")
  public Status() {
  }

  /**
   * @return status The code status
   */
  @JsonProperty("code")
  public Integer getCode() {
    return code;
  }

  /**
   * @param status The status code
   */
  @SuppressWarnings("unused")
  public void setCode(Integer status) {
    this.code = status;
  }

  /**
   * @return The status name
   */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
   * @param name The status name
   */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

}
