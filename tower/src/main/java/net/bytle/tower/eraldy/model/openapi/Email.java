package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An email template
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Email   {


  protected String guid;

  protected String name;

  protected String subject;

  protected String body;

  protected LocalDateTime creationTime;

  protected LocalDateTime modificationTime;

  protected Realm realm;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public Email () {
  }

  /**
  * @return guid The file guid
  */
  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  /**
  * @param guid The file guid
  */
  @SuppressWarnings("unused")
  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
  * @return name the file name (for human)
  */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
  * @param name the file name (for human)
  */
  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  /**
  * @return subject The subject of the email
  */
  @JsonProperty("subject")
  public String getSubject() {
    return subject;
  }

  /**
  * @param subject The subject of the email
  */
  @SuppressWarnings("unused")
  public void setSubject(String subject) {
    this.subject = subject;
  }

  /**
  * @return body The body of the email
  */
  @JsonProperty("body")
  public String getBody() {
    return body;
  }

  /**
  * @param body The body of the email
  */
  @SuppressWarnings("unused")
  public void setBody(String body) {
    this.body = body;
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
    Email email = (Email) o;
    return Objects.equals(guid, email.guid);
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
