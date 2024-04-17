package net.bytle.tower.eraldy.module.mailing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.openapi.ListUser;

import java.time.LocalDateTime;

/**
 * A mailing item to deliver
 */
public class MailingItem {


  private String guid;
  private MailingItemStatus status;

  private Mailing mailing;

  private LocalDateTime creationTime;
  private Integer failureCount;
  private LocalDateTime modificationTime;
  private String statusMessage;
  private MailingJob mailingJob;
  private LocalDateTime emailDate;
  private ListUser listUser;

  public MailingItem() {
  }


  public void setStatus(MailingItemStatus status) {
    this.status = status;
  }

  public void setMailing(Mailing mailing) {
    this.mailing = mailing;
  }


  public void setCreationTime(LocalDateTime creationTime) {
    this.creationTime = creationTime;
  }

  @JsonProperty("mailing")
  public Mailing getMailing() {
    return this.mailing;
  }


  @JsonProperty("guid")
  public String getGuid() {
    return guid;
  }

  @JsonProperty("status")
  public MailingItemStatus getStatus() {
    return this.status;
  }

  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return this.creationTime;
  }

  @JsonProperty("countFailure")
  public Integer getFailureCount() {
    return failureCount;
  }

  public void setFailureCount(Integer failureCount) {
    this.failureCount = failureCount;
  }

  public void setModificationTime(LocalDateTime modificationTime) {
    this.modificationTime = modificationTime;
  }

  @JsonProperty("modificationTime")
  public LocalDateTime getModificationTime() {
    return modificationTime;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  @JsonProperty("statusMessage")
  public String getStatusMessage() {
    return statusMessage;
  }


  public void setGuid(String guidString) {
    this.guid = guidString;
  }

  public void setMailingJob(MailingJob mailingJob) {
    this.mailingJob = mailingJob;
  }

  public void setEmailDate(LocalDateTime emailDate) {
    this.emailDate = emailDate;
  }
  @JsonProperty("mailingJob")
  public MailingJob getMailingJob() {
    return mailingJob;
  }

  @JsonProperty("emailDate")
  public LocalDateTime getEmailDate() {
    return emailDate;
  }

  public void setListUser(ListUser listUser) {
    this.listUser = listUser;
  }

  @JsonProperty("listUser")
  public ListUser getListUser() {
    return listUser;
  }
}
