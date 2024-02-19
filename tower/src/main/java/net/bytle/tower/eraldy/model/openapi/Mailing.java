package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A mailing represents the sending of an email to one or more users
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Mailing   {


  protected Long localId;

  protected String guid;

  protected String name;

  protected OrganizationUser senderUser;

  protected Object emailTemplate = null;

  protected ListObject recipientList;

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
  * @return senderUser
  */
  @JsonProperty("senderUser")
  public OrganizationUser getSenderUser() {
    return senderUser;
  }

  /**
  * @param senderUser Set senderUser
  */
  @SuppressWarnings("unused")
  public void setSenderUser(OrganizationUser senderUser) {
    this.senderUser = senderUser;
  }

  /**
  * @return emailTemplate The body of the email in a template format
  */
  @JsonProperty("emailTemplate")
  public Object getEmailTemplate() {
    return emailTemplate;
  }

  /**
  * @param emailTemplate The body of the email in a template format
  */
  @SuppressWarnings("unused")
  public void setEmailTemplate(Object emailTemplate) {
    this.emailTemplate = emailTemplate;
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
    return
            Objects.equals(guid, mailing.guid);

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
