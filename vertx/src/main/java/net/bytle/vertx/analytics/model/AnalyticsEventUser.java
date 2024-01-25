package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * The user properties for the event They are only the identifier. The AnalyticsUser object has all properties and is used to create a profile
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEventUser   {


  protected String userGuid;

  protected String userEmail;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsEventUser () {
  }

  /**
  * @return userGuid A unique identifier
  */
  @JsonProperty("userGuid")
  public String getUserGuid() {
    return userGuid;
  }

  /**
  * @param userGuid A unique identifier
  */
  @SuppressWarnings("unused")
  public void setUserGuid(String userGuid) {
    this.userGuid = userGuid;
  }

  /**
  * @return userEmail the user email (the user handle, may change for the id)
  */
  @JsonProperty("userEmail")
  public String getUserEmail() {
    return userEmail;
  }

  /**
  * @param userEmail the user email (the user handle, may change for the id)
  */
  @SuppressWarnings("unused")
  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsEventUser analyticsEventUser = (AnalyticsEventUser) o;
    return

            Objects.equals(userGuid, analyticsEventUser.userGuid) && Objects.equals(userEmail, analyticsEventUser.userEmail);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userGuid, userEmail);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
