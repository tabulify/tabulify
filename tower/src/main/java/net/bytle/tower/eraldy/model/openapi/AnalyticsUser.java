package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * The user
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsUser   {


  protected String id;

  protected String givenName;

  protected String email;

  protected URI avatar;

  protected LocalDateTime creationTime;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsUser () {
  }

  /**
  * @return id A unique identifier
  */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /**
  * @param id A unique identifier
  */
  @SuppressWarnings("unused")
  public void setId(String id) {
    this.id = id;
  }

  /**
  * @return givenName the user given name
  */
  @JsonProperty("givenName")
  public String getGivenName() {
    return givenName;
  }

  /**
  * @param givenName the user given name
  */
  @SuppressWarnings("unused")
  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  /**
  * @return email the user email
  */
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  /**
  * @param email the user email
  */
  @SuppressWarnings("unused")
  public void setEmail(String email) {
    this.email = email;
  }

  /**
  * @return avatar the user avatar
  */
  @JsonProperty("avatar")
  public URI getAvatar() {
    return avatar;
  }

  /**
  * @param avatar the user avatar
  */
  @SuppressWarnings("unused")
  public void setAvatar(URI avatar) {
    this.avatar = avatar;
  }

  /**
  * @return creationTime The timestamp when the user was created
  */
  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  /**
  * @param creationTime The timestamp when the user was created
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
    AnalyticsUser analyticsUser = (AnalyticsUser) o;
    return

            Objects.equals(id, analyticsUser.id) && Objects.equals(givenName, analyticsUser.givenName) && Objects.equals(email, analyticsUser.email) && Objects.equals(avatar, analyticsUser.avatar) && Objects.equals(creationTime, analyticsUser.creationTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, givenName, email, avatar, creationTime);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
