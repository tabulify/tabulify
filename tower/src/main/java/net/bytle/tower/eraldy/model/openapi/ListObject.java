package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.type.Handle;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A list where a user can register.  Example: * Newsletter * Sign up to a waiting list
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListObject   {


  protected Long localId;

  protected String guid;

  protected Handle handle;

  protected String name;

  protected String title;

  protected String description;

  protected OrgaUser ownerUser;

  protected App app;


  protected URI registrationUrl;

  protected Long userCount;

  protected Long userInCount;

  protected Long mailingCount;
  private LocalDateTime creationTime;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListObject () {
  }

  /**
  * @return localId The list identifier in the realm scope. Without the realm, this id have duplicate.
  */
  @JsonProperty("localId")
  public Long getLocalId() {
    return localId;
  }

  /**
  * @param localId The list identifier in the realm scope. Without the realm, this id have duplicate.
  */
  @SuppressWarnings("unused")
  public void setLocalId(Long localId) {
    this.localId = localId;
  }

  /**
  * @return guid The global list id where the user can register
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The global list id where the user can register
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return handle A handle (unique code) for the list
  */
  @JsonProperty("handle")
  public Handle getHandle() {
    return handle;
  }

  /**
  * @param handle A handle (unique code) for the list
  */
  @SuppressWarnings("unused")
  public void setHandle(Handle handle) {
    this.handle = handle;
  }

  /**
  * @return name The name of the list
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name The name of the list
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return title The title of the list used in heading
  */
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  /**
  * @param title The title of the list used in heading
  */
  @SuppressWarnings("unused")
  public void setTitle(String title) {
    this.title = title;
  }

  /**
  * @return description The description of the list
  */
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  /**
  * @param description The description of the list
  */
  @SuppressWarnings("unused")
  public void setDescription(String description) {
    this.description = description;
  }

  /**
  * @return ownerUser
  */
  @JsonProperty("ownerUser")
  public OrgaUser getOwnerUser() {
    return ownerUser;
  }

  /**
  * @param ownerUser Set ownerUser
  */
  @SuppressWarnings("unused")
  public void setOwnerUser(OrgaUser ownerUser) {
    this.ownerUser = ownerUser;
  }

  /**
  * @return app
  */
  @JsonProperty("app")
  public App getApp() {
    return app;
  }

  /**
  * @param app Set app
  */
  @SuppressWarnings("unused")
  public void setApp(App app) {
    this.app = app;
  }

  /**
  * @return registrationUrl The public page where to register for this list
  */
  @JsonProperty("registrationUrl")
  public URI getRegistrationUrl() {
    return registrationUrl;
  }

  /**
  * @param registrationUrl The public page where to register for this list
  */
  @SuppressWarnings("unused")
  public void setRegistrationUrl(URI registrationUrl) {
    this.registrationUrl = registrationUrl;
  }

  /**
  * @return userCount The number of users for the list (subscribed/unsubscribed - registered/unregistered)
  */
  @JsonProperty("userCount")
  public Long getUserCount() {
    return userCount;
  }

  /**
  * @param userCount The number of users for the list (subscribed/unsubscribed - registered/unregistered)
  */
  @SuppressWarnings("unused")
  public void setUserCount(Long userCount) {
    this.userCount = userCount;
  }

  /**
  * @return userInCount The number of users that are still in the list
  */
  @JsonProperty("userInCount")
  public Long getUserInCount() {
    return userInCount;
  }

  /**
  * @param userInCount The number of users that are still in the list
  */
  @SuppressWarnings("unused")
  public void setUserInCount(Long userInCount) {
    this.userInCount = userInCount;
  }

  /**
  * @return mailingCount The number of mailing for this list
  */
  @JsonProperty("mailingCount")
  public Long getMailingCount() {
    return mailingCount;
  }

  /**
  * @param mailingCount The number of mailing for this list
  */
  @SuppressWarnings("unused")
  public void setMailingCount(Long mailingCount) {
    this.mailingCount = mailingCount;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListObject listObject = (ListObject) o;
    return Objects.equals(guid, listObject.guid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid);
  }

  @Override
  public String toString() {
    return guid + ", " + handle;
  }

  public void setCreationTime(LocalDateTime localDateTime) {
    this.creationTime = localDateTime;
  }

  public LocalDateTime getCreationTime() {
    return this.creationTime;
  }
}
