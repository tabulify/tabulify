package net.bytle.tower.eraldy.model.manual;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * A mailing execution that sends email
 */
public class MailingJob {


  private MailingJobStatus status;
  private Long localId;
  private Mailing mailing;
  private String guid;
  private LocalDateTime startTime;
  private Long countRowToExecute;
  private LocalDateTime endTime;
  private Long countRowSuccess;
  private Long countRowExecution;
  private String statusMessage;

  public MailingJob() {
  }


  public void setStatus(MailingJobStatus mailingJobStatus) {
    this.status = mailingJobStatus;
  }

  public void setLocalId(Long localId) {
    this.localId = localId;
  }

  public void setMailing(Mailing mailing) {
    this.mailing = mailing;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  @JsonProperty("mailing")
  public Mailing getMailing() {
    return this.mailing;
  }

  public Long getLocalId() {
    return this.localId;
  }

  @JsonProperty("guid")
  public String getGuid() {
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
  public Long getCountRowToExecute() {
    return countRowToExecute;
  }

  public void setCountRowToExecute(Long rowCountToExecute) {
    this.countRowToExecute = rowCountToExecute;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public void setCountRowSuccess(Long rowCountSuccess) {
    this.countRowSuccess = rowCountSuccess;
  }

  public void setCountRowExecution(Long rowCountExecution) {
    this.countRowExecution = rowCountExecution;
  }

  @JsonProperty("endTime")
  public LocalDateTime getEndTime() {
    return endTime;
  }

  @JsonProperty("countRowSuccess")
  public Long getCountRowSuccess() {
    return countRowSuccess;
  }

  @JsonProperty("countRowExecution")
  public Long getCountRowExecution() {
    return countRowExecution;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  @JsonProperty("statusMessage")
  public String getStatusMessage() {
    return statusMessage;
  }
}
