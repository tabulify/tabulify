package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * List Analytics (count, ...)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListObjectAnalytics extends ListObject  {


  protected Integer userCount;

  protected Integer userInCount;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListObjectAnalytics () {
  }

  /**
  * @return userCount The number of users for the list (subscribed/unsubscribed - registered/unregistered)
  */
  @JsonProperty("userCount")
  public Integer getUserCount() {
    return userCount;
  }

  /**
  * @param userCount The number of users for the list (subscribed/unsubscribed - registered/unregistered)
  */
  @SuppressWarnings("unused")
  public void setUserCount(Integer userCount) {
    this.userCount = userCount;
  }

  /**
  * @return userInCount The number of users that are still in the list
  */
  @JsonProperty("userInCount")
  public Integer getUserInCount() {
    return userInCount;
  }

  /**
  * @param userInCount The number of users that are still in the list
  */
  @SuppressWarnings("unused")
  public void setUserInCount(Integer userInCount) {
    this.userInCount = userInCount;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListObjectAnalytics listObjectAnalytics = (ListObjectAnalytics) o;
    return super.equals(o) &&

            Objects.equals(userCount, listObjectAnalytics.userCount) && Objects.equals(userInCount, listObjectAnalytics.userInCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), userCount, userInCount);
  }

  @Override
  public String toString() {
    return super.toString() + super.toString();
  }

}
