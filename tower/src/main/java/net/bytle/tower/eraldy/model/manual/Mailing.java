package net.bytle.tower.eraldy.model.manual;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.openapi.ListObject;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.Realm;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A mailing represents the sending of an email to one or more users
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Mailing {


  protected Long localId;

  protected String guid;

  protected String name;

  protected String emailSubject;
  protected String emailBody;
  protected String emailPreview;

  protected OrganizationUser emailAuthor;

  protected ListObject emailRecipientList;

  protected MailingStatus status;

  protected LocalDateTime creationTime;

  protected LocalDateTime modificationTime;

  protected Realm realm;
  private String emailLanguage;
  private Long countRow;
  private Long countRowSuccess;
  private Long countRowExecution;
  private LocalDateTime jobLastExecutionTime;
  private LocalDateTime jobNextExecutionTime;

  /**
   * The empty constructor is
   * needed for the construction of the pojo
   * with the Jackson library
   */
  @SuppressWarnings("unused")
  public Mailing() {
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
   * @return emailSubject The email subject
   */
  @JsonProperty("emailSubject")
  public String getEmailSubject() {
    return emailSubject;
  }

  /**
   * @param emailSubject The email subject
   */
  @SuppressWarnings("unused")
  public void setEmailSubject(String emailSubject) {
    this.emailSubject = emailSubject;
  }

  /**
   * @return emailBody The email body
   */
  @JsonProperty("emailBody")
  public String getEmailBody() {
    return emailBody;
  }

  /**
   * @param emailBody The email subject
   */
  @SuppressWarnings("unused")
  public void setEmailBody(String emailBody) {
    this.emailBody = emailBody;
  }

  /**
   * @return emailPreview The email preview
   */
  @JsonProperty("emailPreview")
  public String getEmailPreview() {
    return emailPreview;
  }

  /**
   * @param emailPreview The email subject
   */
  @SuppressWarnings("unused")
  public void setEmailPreview(String emailPreview) {
    this.emailPreview = emailPreview;
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
  public ListObject getEmailRecipientList() {
    return emailRecipientList;
  }

  /**
   * @param emailRecipientList Set recipientList
   */
  @SuppressWarnings("unused")
  public void setEmailRecipientList(ListObject emailRecipientList) {
    this.emailRecipientList = emailRecipientList;
  }

  /**
   * @return status The status of the mailing
   */
  @JsonProperty("status")
  public MailingStatus getStatus() {
    return status;
  }

  /**
   * @param status The status of the mailing
   */
  @SuppressWarnings("unused")
  public void setStatus(MailingStatus status) {
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

  @JsonProperty("emailLanguage")
  public String getEmailLanguage() {
    return emailLanguage;
  }

  @SuppressWarnings("unused")
  public void setEmailLanguage(String emailLanguage) {
    this.emailLanguage = emailLanguage;
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

  /**
   * @param countRow - The total number of email address s to send
   */
  public void setCountRow(Long countRow) {
    this.countRow = countRow;
  }

  /**
   * @return The total number of user to whom we need to send an email
   */
  @JsonProperty("countRow")
  public Long getCountRow() {
    return countRow;
  }

  /**
   * @param countEmailSuccess - the number of email send successfully
   */
  public void setCountRowSuccess(Long countEmailSuccess) {
    this.countRowSuccess = countEmailSuccess;
  }

  /**
   * @param countExecution - the number of time we executed an SMTP transaction (independently of the status such as success and failure)
   */
  public void setCountRowExecution(Long countExecution) {
    this.countRowExecution = countExecution;
  }

  /**
   * @return the number of email send successfully to the user
   */
  @JsonProperty("countRowSuccess")
  public Long getCountRowSuccess() {
    return countRowSuccess;
  }

  /**
   *
   * @return the number of smtp transactions
   */
  @JsonProperty("countRowExecution")
  public Long getCountRowExecution() {
    return countRowExecution;
  }

  /**
   * @param dateTime The last time that the mailing job was executed
   */
  public void setJobLastExecutionTime(LocalDateTime dateTime) {
    this.jobLastExecutionTime = dateTime;
  }

  /**
   *
   * @return The last time that a mailing job  was executed
   */
  @JsonProperty("jobLastExecutionTime")
  public LocalDateTime getJobLastExecutionTime() {
    return jobLastExecutionTime;
  }

  /**
   * @param dateTime The next time that a mailing job should be executed
   */
  public void setJobNextExecutionTime(LocalDateTime dateTime) {
    this.jobNextExecutionTime = dateTime;
  }

  /**
   *
   * @return The next time that a mailing job should be executed
   */
  @JsonProperty("jobNextExecutionTime")
  public LocalDateTime getJobNextExecutionTime() {
    return jobNextExecutionTime;
  }

}
