package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Realm Analytics (count, ...)
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RealmAnalytics extends Realm  {


  protected Integer userCount;

  protected Integer appCount;

  protected Integer listCount;

  protected Integer subscriberCount;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public RealmAnalytics () {
  }

  /**
  * @return userCount The number of users for the realm
  */
  @JsonProperty("userCount")
  public Integer getUserCount() {
    return userCount;
  }

  /**
  * @param userCount The number of users for the realm
  */
  @SuppressWarnings("unused")
  public void setUserCount(Integer userCount) {
    this.userCount = userCount;
  }

  /**
  * @return appCount The number of apps for the realm
  */
  @JsonProperty("appCount")
  public Integer getAppCount() {
    return appCount;
  }

  /**
  * @param appCount The number of apps for the realm
  */
  @SuppressWarnings("unused")
  public void setAppCount(Integer appCount) {
    this.appCount = appCount;
  }

  /**
  * @return listCount The number of lists for the realm
  */
  @JsonProperty("listCount")
  public Integer getListCount() {
    return listCount;
  }

  /**
  * @param listCount The number of lists for the realm
  */
  @SuppressWarnings("unused")
  public void setListCount(Integer listCount) {
    this.listCount = listCount;
  }

  /**
  * @return subscriberCount The number of subscribers for the realm
  */
  @JsonProperty("subscriberCount")
  public Integer getSubscriberCount() {
    return subscriberCount;
  }

  /**
  * @param subscriberCount The number of subscribers for the realm
  */
  @SuppressWarnings("unused")
  public void setSubscriberCount(Integer subscriberCount) {
    this.subscriberCount = subscriberCount;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RealmAnalytics realmAnalytics = (RealmAnalytics) o;
    return super.equals(o) && Objects.equals(userCount, realmAnalytics.userCount) &&
        Objects.equals(appCount, realmAnalytics.appCount) &&
        Objects.equals(listCount, realmAnalytics.listCount) &&
        Objects.equals(subscriberCount, realmAnalytics.subscriberCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), guid, super.hashCode(), handle);
  }

  @Override
  public String toString() {
    return "class RealmAnalytics {\n" +
    "    " + toIndentedString(super.toString()) + "\n" +

    "    guid: " + toIndentedString(guid) + "\n" +

    "    handle: " + toIndentedString(handle) + "\n" +
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
