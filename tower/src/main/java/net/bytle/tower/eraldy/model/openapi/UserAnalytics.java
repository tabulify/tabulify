package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * User and Analytics
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAnalytics extends User  {

  private LocalDateTime lastActiveTime;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public UserAnalytics () {
  }

  /**
  * @return lastActiveTime the last active time (should be on date level)
  */
  @JsonProperty("lastActiveTime")
  public LocalDateTime getLastActiveTime() {
    return lastActiveTime;
  }

  /**
  * @param lastActiveTime the last active time (should be on date level)
  */
  @SuppressWarnings("unused")
  public void setLastActiveTime(LocalDateTime lastActiveTime) {
    this.lastActiveTime = lastActiveTime;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserAnalytics userAnalytics = (UserAnalytics) o;
    return super.equals(o) && Objects.equals(lastActiveTime, userAnalytics.lastActiveTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), lastActiveTime);
  }

  @Override
  public String toString() {
    return "class UserAnalytics {\n" +
    "    " + toIndentedString(super.toString()) + "\n" +
    "}";
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
