package net.bytle.tower.eraldy.module.realm.inputs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.realm.model.UserStatus;
import net.bytle.type.EmailAddress;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.TimeZone;

/**
 * A input props user
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInputProps {




  protected EmailAddress emailAddress;

  protected String givenName;

  protected String familyName;

  protected String title;

  protected UserStatus status;

  protected String statusMessage;

  protected URI avatar;

  protected String bio;

  protected URI website;

  protected String location;

  protected TimeZone timeZone;

  protected LocalDateTime lastActiveTime;


  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public UserInputProps() {
  }

  /**
  * @return emailAddress The email address of the user
  */
  @JsonProperty("emailAddress")
  public EmailAddress getEmailAddress() {
    return emailAddress;
  }

  /**
  * @param emailAddress The email address of the user
  */
  @SuppressWarnings("unused")
  public void setEmailAddress(EmailAddress emailAddress) {
    this.emailAddress = emailAddress;
  }

  /**
  * @return givenName The short and informal name of the user (used in signature, known also as the given, calling or first name)
  */
  @com.fasterxml.jackson.annotation.JsonAlias({"name"})
  @JsonProperty("givenName")
  public String getGivenName() {
    return givenName;
  }

  /**
  * @param givenName The short and informal name of the user (used in signature, known also as the given, calling or first name)
  */
  @SuppressWarnings("unused")
  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  /**
  * @return familyName The family name of the user
  */
  @com.fasterxml.jackson.annotation.JsonAlias({"fullname","fullName"})
  @JsonProperty("familyName")
  public String getFamilyName() {
    return familyName;
  }

  /**
  * @param familyName The family name of the user
  */
  @SuppressWarnings("unused")
  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  /**
  * @return title The title of the user (a short user description such as a role used in signature)
  */
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  /**
  * @param title The title of the user (a short user description such as a role used in signature)
  */
  @SuppressWarnings("unused")
  public void setTitle(String title) {
    this.title = title;
  }

  /**
  * @return status Status code (Soft delete, ...)
  */
  @JsonProperty("status")
  public UserStatus getStatus() {
    return status;
  }

  /**
  * @param status Status code (Soft delete, ...)
  */
  @SuppressWarnings("unused")
  public void setStatus(UserStatus status) {
    this.status = status;
  }


  @JsonProperty("statusMessage")
  public String getStatusMessage() {
    return statusMessage;
  }

  /**
  * @param statusMessage The status message (the reason)
  */
  @SuppressWarnings("unused")
  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  /**
  * @return avatar The avatar image of the user
  */
  @JsonProperty("avatar")
  public URI getAvatar() {
    return avatar;
  }

  /**
  * @param avatar The avatar image of the user
  */
  @SuppressWarnings("unused")
  public void setAvatar(URI avatar) {
    this.avatar = avatar;
  }

  /**
  * @return bio A description for the user
  */
  @JsonProperty("bio")
  public String getBio() {
    return bio;
  }

  /**
  * @param bio A description for the user
  */
  @SuppressWarnings("unused")
  public void setBio(String bio) {
    this.bio = bio;
  }

  /**
  * @return website A link to a user website
  */
  @JsonProperty("website")
  public URI getWebsite() {
    return website;
  }

  /**
  * @param website A link to a user website
  */
  @SuppressWarnings("unused")
  public void setWebsite(URI website) {
    this.website = website;
  }

  /**
  * @return location The location of the user A free text that the user updates
  */
  @JsonProperty("location")
  public String getLocation() {
    return location;
  }

  /**
  * @param location The location of the user A free text that the user updates
  */
  @SuppressWarnings("unused")
  public void setLocation(String location) {
    this.location = location;
  }

  /**
  * @return timeZone The timezone full name (In Java, the timezone id)
  */
  @JsonProperty("timeZone")
  public TimeZone getTimeZone() {
    return timeZone;
  }

  /**
  * @param timeZone The timezone full name (In Java, the timezone id)
  */
  @SuppressWarnings("unused")
  public void setTimeZone(TimeZone timeZone) {
    this.timeZone = timeZone;
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


}
