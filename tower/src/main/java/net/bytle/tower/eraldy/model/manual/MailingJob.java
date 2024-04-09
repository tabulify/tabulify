package net.bytle.tower.eraldy.model.manual;

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

  public Mailing getMailing() {
    return this.mailing;
  }

  public Long getLocalId() {
    return this.localId;
  }

  public String getGuid() {
    return guid;
  }

  public MailingJobStatus getStatus() {
    return this.status;
  }

  public LocalDateTime getStartTime() {
    return this.startTime;
  }

  public Integer getRowCountToExecute() {
    return null;
  }

}
