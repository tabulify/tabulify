package net.bytle.tower.eraldy.module.mailing.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * A mailing execution that sends email
 */
public class MailingJob {


  private MailingJobStatus status;
  private Mailing mailing;
  private MailingJobGuid guid;
  private LocalDateTime startTime;
  private Long itemToExecuteCount;
  private LocalDateTime endTime;
  private Long itemSuccessCount;
  private Long itemExecutionCount;
  private String statusMessage;

  public MailingJob() {
  }


  public void setStatus(MailingJobStatus mailingJobStatus) {
    this.status = mailingJobStatus;
  }

  public void setMailing(Mailing mailing) {
    this.mailing = mailing;
  }

  public void setGuid(MailingJobGuid guid) {
    this.guid = guid;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  @JsonProperty("mailing")
  public Mailing getMailing() {
    return this.mailing;
  }

  @JsonProperty("guid")
  public MailingJobGuid getGuid() {
    return guid;
  }

  @JsonProperty("status")
  public MailingJobStatus getStatus() {
    return this.status;
  }

  @JsonProperty("startTime")
  public LocalDateTime getStartTime() {
    return this.startTime;
  }

  @JsonProperty("countRowToExecute")
  public Long getItemToExecuteCount() {
    return itemToExecuteCount;
  }

  public void setItemToExecuteCount(Long rowCountToExecute) {
    this.itemToExecuteCount = rowCountToExecute;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public void setItemSuccessCount(Long rowCountSuccess) {
    this.itemSuccessCount = rowCountSuccess;
  }

  public void setItemExecutionCount(Long rowCountExecution) {
    this.itemExecutionCount = rowCountExecution;
  }

  @JsonProperty("endTime")
  public LocalDateTime getEndTime() {
    return endTime;
  }

  @JsonProperty("countRowSuccess")
  public Long getItemSuccessCount() {
    return itemSuccessCount;
  }

  @JsonProperty("countRowExecution")
  public Long getItemExecutionCount() {
    return itemExecutionCount;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  @JsonProperty("statusMessage")
  public String getStatusMessage() {
    return statusMessage;
  }
}
