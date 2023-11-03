package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A user
 **/
@com.fasterxml.jackson.annotation.JsonIdentityInfo( generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class, property = "guid", scope = User.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User   {


  protected String guid;

  protected Long localId;

  protected String handle;

  protected String email;

  protected String name;

  protected String fullname;

  protected String title;

  protected Boolean disabled;

  protected String disabledReason;

  protected URI avatar;

  protected String bio;

  protected URI website;

  protected String location;

  protected LocalDateTime creationTime;

  protected LocalDateTime modificationTime;

  protected Realm realm;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public User () {
  }

  /**
  * @return guid It is the global identifier * a string representation of realm id + user local id * never changes. It's the public id that: * you can see in the url * send to external application such as Analytics SAAS provider.  The database id (ie realm id + user local id) is not public.
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid It is the global identifier * a string representation of realm id + user local id * never changes. It's the public id that: * you can see in the url * send to external application such as Analytics SAAS provider.  The database id (ie realm id + user local id) is not public.
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return localId The user id in the realm in the database (ie local to the realm)  We have called it local to avoid to really indicate that is not the true id.  You can't check with this id if this is the same user as this is the id inside the realm, there is other id with the same value in another realm.
  */
  @JsonProperty("localId")
  public Long getLocalId() {
    return localId;
  }

  /**
  * @param localId The user id in the realm in the database (ie local to the realm)  We have called it local to avoid to really indicate that is not the true id.  You can't check with this id if this is the same user as this is the id inside the realm, there is other id with the same value in another realm.
  */
  @SuppressWarnings("unused")
  public void setLocalId(Long localId) {
    this.localId = localId;
  }

  /**
  * @return handle The handle of the user
  */
  @JsonProperty("handle")
  public String getHandle() {
    return handle;
  }

  /**
  * @param handle The handle of the user
  */
  @SuppressWarnings("unused")
  public void setHandle(String handle) {
    this.handle = handle;
  }

  /**
  * @return email The email of the user
  */
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  /**
  * @param email The email of the user
  */
  @SuppressWarnings("unused")
  public void setEmail(String email) {
    this.email = email;
  }

  /**
  * @return name The short and informal name of the user (used in signature, known also as the given, calling or first name)
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name The short and informal name of the user (used in signature, known also as the given, calling or first name)
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return fullname The long and formal name of the user (used in address)
  */
  @JsonProperty("fullname")
  public String getFullname() {
    return fullname;
  }

  /**
  * @param fullname The long and formal name of the user (used in address)
  */
  @SuppressWarnings("unused")
  public void setFullname(String fullname) {
    this.fullname = fullname;
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
  * @return disabled Soft deleted
  */
  @JsonProperty("disabled")
  public Boolean getDisabled() {
    return disabled;
  }

  /**
  * @param disabled Soft deleted
  */
  @SuppressWarnings("unused")
  public void setDisabled(Boolean disabled) {
    this.disabled = disabled;
  }

  /**
  * @return disabledReason The reason of the soft delete
  */
  @JsonProperty("disabledReason")
  public String getDisabledReason() {
    return disabledReason;
  }

  /**
  * @param disabledReason The reason of the soft delete
  */
  @SuppressWarnings("unused")
  public void setDisabledReason(String disabledReason) {
    this.disabledReason = disabledReason;
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
  * @return location The location of the user
  */
  @JsonProperty("location")
  public String getLocation() {
    return location;
  }

  /**
  * @param location The location of the user
  */
  @SuppressWarnings("unused")
  public void setLocation(String location) {
    this.location = location;
  }

  /**
  * @return creationTime The creation time of the user
  */
  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  /**
  * @param creationTime The creation time of the user
  */
  @SuppressWarnings("unused")
  public void setCreationTime(LocalDateTime creationTime) {
    this.creationTime = creationTime;
  }

  /**
  * @return modificationTime The last modification time of the user
  */
  @JsonProperty("modificationTime")
  public LocalDateTime getModificationTime() {
    return modificationTime;
  }

  /**
  * @param modificationTime The last modification time of the user
  */
  @SuppressWarnings("unused")
  public void setModificationTime(LocalDateTime modificationTime) {
    this.modificationTime = modificationTime;
  }

  /**
  * @return realm
  */
  @JsonProperty("realm")
  public Realm getRealm() {
    return realm;
  }

  /**
  * @param realm Set realm
  */
  @SuppressWarnings("unused")
  public void setRealm(Realm realm) {
    this.realm = realm;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    User user = (User) o;
    return Objects.equals(guid, user.guid) &&
        Objects.equals(localId, user.localId) &&
        Objects.equals(handle, user.handle) &&
        Objects.equals(email, user.email) &&
        Objects.equals(name, user.name) &&
        Objects.equals(fullname, user.fullname) &&
        Objects.equals(title, user.title) &&
        Objects.equals(disabled, user.disabled) &&
        Objects.equals(disabledReason, user.disabledReason) &&
        Objects.equals(avatar, user.avatar) &&
        Objects.equals(bio, user.bio) &&
        Objects.equals(website, user.website) &&
        Objects.equals(location, user.location) &&
        Objects.equals(creationTime, user.creationTime) &&
        Objects.equals(modificationTime, user.modificationTime) &&
        Objects.equals(realm, user.realm);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid, localId, handle, email, name, fullname, title, disabled, disabledReason, avatar, bio, website, location, creationTime, modificationTime, realm);
  }

  @Override
  public String toString() {
    return "class User {\n" +

    "    guid: " + toIndentedString(guid) + "\n" +

    "    handle: " + toIndentedString(handle) + "\n" +

    "    email: " + toIndentedString(email) + "\n" +
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
