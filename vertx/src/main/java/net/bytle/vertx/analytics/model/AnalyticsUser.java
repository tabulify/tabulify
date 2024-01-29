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

  protected String realmGuid;

  protected String realmHandle;

  protected String organizationGuid;

  protected String organizationHandle;

  protected LocalDateTime creationTime;

  protected String remoteIp;

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
  * @return realmGuid the realm guid
  */
  @JsonProperty("realmGuid")
  public String getRealmGuid() {
    return realmGuid;
  }

  /**
  * @param realmGuid the realm guid
  */
  @SuppressWarnings("unused")
  public void setRealmGuid(String realmGuid) {
    this.realmGuid = realmGuid;
  }

  /**
  * @return realmHandle the realm handle
  */
  @JsonProperty("realmHandle")
  public String getRealmHandle() {
    return realmHandle;
  }

  /**
  * @param realmHandle the realm handle
  */
  @SuppressWarnings("unused")
  public void setRealmHandle(String realmHandle) {
    this.realmHandle = realmHandle;
  }

  /**
  * @return organizationGuid the organization guid
  */
  @JsonProperty("organizationGuid")
  public String getOrganizationGuid() {
    return organizationGuid;
  }

  /**
  * @param organizationGuid the organization guid
  */
  @SuppressWarnings("unused")
  public void setOrganizationGuid(String organizationGuid) {
    this.organizationGuid = organizationGuid;
  }

  /**
  * @return organizationHandle the organization handle
  */
  @JsonProperty("organizationHandle")
  public String getOrganizationHandle() {
    return organizationHandle;
  }

  /**
  * @param organizationHandle the organization handle
  */
  @SuppressWarnings("unused")
  public void setOrganizationHandle(String organizationHandle) {
    this.organizationHandle = organizationHandle;
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

  /**
  * @return remoteIp The request remote Ip to update the location
  */
  @JsonProperty("remoteIp")
  public String getRemoteIp() {
    return remoteIp;
  }

  /**
  * @param remoteIp The request remote Ip to update the location
  */
  @SuppressWarnings("unused")
  public void setRemoteIp(String remoteIp) {
    this.remoteIp = remoteIp;
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

            Objects.equals(guid, analyticsUser.guid) && Objects.equals(name, analyticsUser.name) && Objects.equals(givenName, analyticsUser.givenName) && Objects.equals(familyName, analyticsUser.familyName) && Objects.equals(email, analyticsUser.email) && Objects.equals(avatar, analyticsUser.avatar) && Objects.equals(realmGuid, analyticsUser.realmGuid) && Objects.equals(realmHandle, analyticsUser.realmHandle) && Objects.equals(organizationGuid, analyticsUser.organizationGuid) && Objects.equals(organizationHandle, analyticsUser.organizationHandle) && Objects.equals(creationTime, analyticsUser.creationTime) && Objects.equals(remoteIp, analyticsUser.remoteIp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, name, givenName, familyName, email, avatar, realmGuid, realmHandle, organizationGuid, organizationHandle, creationTime, remoteIp);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
