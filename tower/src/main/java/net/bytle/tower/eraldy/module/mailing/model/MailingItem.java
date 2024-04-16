package net.bytle.tower.eraldy.module.mailing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.openapi.User;

import java.time.LocalDateTime;

/**
 * A mailing item to deliver
 */
public class MailingItem {


  private String guid;
  private MailingRowStatus status;

  private Mailing mailing;
  private User user;

  private LocalDateTime creationTime;
  private Long failureCount;
  private LocalDateTime modificationTime;
  private String statusMessage;

  public MailingItem() {
  }


  public void setStatus(MailingRowStatus status) {
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
  public MailingRowStatus getStatus() {
    return this.status;
  }

  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return this.creationTime;
  }

  @JsonProperty("countFailure")
  public Long getFailureCount() {
    return failureCount;
  }

  public void setFailureCount(Long rowCountToExecute) {
    this.failureCount = rowCountToExecute;
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

}
