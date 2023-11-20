package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A list summary
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListSummary   {


  protected String guid;

  protected String handle;

  protected String appUri;

  protected Integer subscriberCount;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListSummary () {
  }

  /**
  * @return guid The public list id
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The public list id
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return handle The handle of the list
  */
  @JsonProperty("handle")
  public String getHandle() {
    return handle;
  }

  /**
  * @param handle The handle of the list
  */
  @SuppressWarnings("unused")
  public void setHandle(String handle) {
    this.handle = handle;
  }

  /**
  * @return appUri The app uri
  */
  @JsonProperty("appUri")
  public String getAppUri() {
    return appUri;
  }

  /**
  * @param appUri The app uri
  */
  @SuppressWarnings("unused")
  public void setAppUri(String appUri) {
    this.appUri = appUri;
  }

  /**
  * @return subscriberCount The number of subscribers
  */
  @JsonProperty("subscriberCount")
  public Integer getSubscriberCount() {
    return subscriberCount;
  }

  /**
  * @param subscriberCount The number of subscribers
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
    ListSummary listSummary = (ListSummary) o;
    return Objects.equals(guid, listSummary.guid) &&
        Objects.equals(handle, listSummary.handle) &&
        Objects.equals(appUri, listSummary.appUri) &&
        Objects.equals(subscriberCount, listSummary.subscriberCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash();
  }

  @Override
  public String toString() {
    return "class ListSummary {\n" +
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
