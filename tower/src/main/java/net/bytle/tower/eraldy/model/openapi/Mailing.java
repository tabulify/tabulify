package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A mailing represents the sending of an email to one or more users
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Mailing   {


  protected Long localId;

  protected String guid;

  protected String name;

  protected Long emailFileId;

  protected OrganizationUser emailAuthor;

  protected ListObject recipientList;

  protected Integer status;

  protected LocalDateTime creationTime;

  protected LocalDateTime modificationTime;

  protected Realm realm;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public Mailing () {
  }

  /**
  * @return localId The mailing id in the database
  */
  @JsonProperty("localId")
  public Long getLocalId() {
    return localId;
  }

  /**
  * @param localId The mailing id in the database
  */
  @SuppressWarnings("unused")
  public void setLocalId(Long localId) {
    this.localId = localId;
  }

  /**
  * @return guid The public id (derived from the database/local id)
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The public id (derived from the database/local id)
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return name A short description of the mailing
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name A short description of the mailing
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return emailFileId The email file id (body, subject, preview)
  */
  @JsonProperty("emailFileId")
  public Long getEmailFileId() {
    return emailFileId;
  }

  /**
  * @param emailFileId The email file id (body, subject, preview)
  */
  @SuppressWarnings("unused")
  public void setEmailFileId(Long emailFileId) {
    this.emailFileId = emailFileId;
  }

  /**
  * @return emailAuthor
  */
  @JsonProperty("emailAuthor")
  public OrganizationUser getEmailAuthor() {
    return emailAuthor;
  }

  /**
  * @param emailAuthor Set emailAuthor
  */
  @SuppressWarnings("unused")
  public void setEmailAuthor(OrganizationUser emailAuthor) {
    this.emailAuthor = emailAuthor;
  }

  /**
  * @return recipientList
  */
  @JsonProperty("recipientList")
  public ListObject getRecipientList() {
    return recipientList;
  }

  /**
  * @param recipientList Set recipientList
  */
  @SuppressWarnings("unused")
  public void setRecipientList(ListObject recipientList) {
    this.recipientList = recipientList;
  }

  /**
  * @return status The status of the mailing
  */
  @JsonProperty("status")
  public Integer getStatus() {
    return status;
  }

  /**
  * @param status The status of the mailing
  */
  @SuppressWarnings("unused")
  public void setStatus(Integer status) {
    this.status = status;
  }

  /**
  * @return creationTime The creation time of the mailing in UTC
  */
  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  /**
  * @param creationTime The creation time of the mailing in UTC
  */
  @SuppressWarnings("unused")
  public void setCreationTime(LocalDateTime creationTime) {
    this.creationTime = creationTime;
  }

  /**
  * @return modificationTime The last modification time of the mailing in UTC
  */
  @JsonProperty("modificationTime")
  public LocalDateTime getModificationTime() {
    return modificationTime;
  }

  /**
  * @param modificationTime The last modification time of the mailing in UTC
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
    Mailing mailing = (Mailing) o;
    return Objects.equals(guid, mailing.guid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(guid);
  }

  @Override
  public String toString() {
    return guid + ", " + name;
  }

}
