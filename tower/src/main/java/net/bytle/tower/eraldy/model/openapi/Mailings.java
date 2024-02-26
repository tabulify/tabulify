package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Dailing data send for a list a mailing (ie mailings)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Mailings   {


  protected String guid;

  protected String name;

  protected Integer status;

  protected LocalDateTime creationTime;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public Mailings () {
  }

  /**
  * @return guid The public id (derived from the database/local id)
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The public id (derived from the database/local id)
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return name A short description of the mailing
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name A short description of the mailing
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return status The status of the mailing
  */
  @JsonProperty("status")
  public Integer getStatus() {
    return status;
  }

  /**
  * @param status The status of the mailing
  */
  @SuppressWarnings("unused")
  public void setStatus(Integer status) {
    this.status = status;
  }

  /**
  * @return creationTime The creation time of the mailing in UTC
  */
  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  /**
  * @param creationTime The creation time of the mailing in UTC
  */
  @SuppressWarnings("unused")
  public void setCreationTime(LocalDateTime creationTime) {
    this.creationTime = creationTime;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Mailings mailings = (Mailings) o;
    return Objects.equals(guid, mailings.guid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid);
  }

  @Override
  public String toString() {
    return guid + ", " + name;
  }

}
