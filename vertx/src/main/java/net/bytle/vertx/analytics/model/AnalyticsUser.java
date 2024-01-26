package net.bytle.vertx.analytics.model;

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


  protected String guid;

  protected String name;

  protected String givenName;

  protected String familyName;

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
  * @return guid A unique global identifier
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid A unique global identifier
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return name The short name
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name The short name
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
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
  * @return familyName the user family name
  */
  @JsonProperty("familyName")
  public String getFamilyName() {
    return familyName;
  }

  /**
  * @param familyName the user family name
  */
  @SuppressWarnings("unused")
  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  /**
  * @return email the user email (the human identifier, ie same as an handle, may change for the id)
  */
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  /**
  * @param email the user email (the human identifier, ie same as an handle, may change for the id)
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

            Objects.equals(guid, analyticsUser.guid) && Objects.equals(name, analyticsUser.name) && Objects.equals(givenName, analyticsUser.givenName) && Objects.equals(familyName, analyticsUser.familyName) && Objects.equals(email, analyticsUser.email) && Objects.equals(avatar, analyticsUser.avatar) && Objects.equals(creationTime, analyticsUser.creationTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, name, givenName, familyName, email, avatar, creationTime);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
