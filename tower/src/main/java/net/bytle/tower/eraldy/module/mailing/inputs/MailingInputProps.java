package net.bytle.tower.eraldy.module.mailing.inputs;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.mailing.model.MailingStatus;

import java.time.LocalDateTime;

/**
 * Mailing Input Props
 * for the creation and or modification of a mailing
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MailingInputProps {

  protected String guid;

  protected String name;

  protected MailingStatus status;
  private String emailSubject;
  private String emailPreview;
  private String emailBody;
  private String emailAuthorGuid;
  private String emailLanguage;
  private LocalDateTime jobNextExecutionTime;


  /**
   * The empty constructor is
   * needed for the construction of the pojo
   * with the Jackson library
   */
  @SuppressWarnings("unused")
  public MailingInputProps () {
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


  @JsonProperty("emailSubject")
  public String getEmailSubject() {
    return emailSubject;
  }


  @SuppressWarnings("unused")
  public void setEmailSubject(String emailSubject) {
    this.emailSubject = emailSubject;
  }

  @JsonProperty("emailLanguage")
  public String getEmailLanguage() {
    return emailLanguage;
  }

  @SuppressWarnings("unused")
  public void setEmailLanguage(String emailLanguage) {
    this.emailLanguage = emailLanguage;
  }

  @JsonProperty("emailPreview")
  public String getEmailPreview() {
    return this.emailPreview;
  }

  @SuppressWarnings("unused")
  public void setEmailPreview(String emailPreview) {
    this.emailPreview = emailPreview;
  }

  @JsonProperty("emailBody")
  public String getEmailBody() {
    return this.emailBody;
  }

  @SuppressWarnings("unused")
  public void setEmailBody(String emailBody) {
    this.emailBody = emailBody;
  }

  @JsonProperty("emailAuthorGuid")
  public String getEmailAuthorGuid() {
    return this.emailAuthorGuid;
  }

  @SuppressWarnings("unused")
  public void setEmailAuthorGuid(String emailAuthorGuid) {
    this.emailAuthorGuid = emailAuthorGuid;
  }

  @JsonProperty("status")
  public MailingStatus getStatus() {
    return this.status;
  }

  @SuppressWarnings("unused")
  public void setStatus(MailingStatus status) {
    this.status = status;
  }

  @JsonProperty("jobNextExecutionTime")
  public LocalDateTime getJobNextExecutionTime() {
    return this.jobNextExecutionTime;
  }

  @SuppressWarnings("unused")
  public void setJobNextExecutionTime(LocalDateTime jobNextExecutionTime) {
    this.jobNextExecutionTime = jobNextExecutionTime;
  }


}
