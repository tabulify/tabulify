package net.bytle.tower.eraldy.module.mailing.inputs;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.mailing.model.MailingItemStatus;
import net.bytle.tower.eraldy.module.mailing.model.MailingJob;

import java.time.LocalDateTime;

/**
 * Mailing Item Input Props
 * for the creation and or modification of a mailing item
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MailingItemInputProps {




  protected MailingItemStatus status;
  private String messageId;
  private String emailServerReceiver;
  private String emailServerSender;



  private MailingJob mailingJobId;
  private Integer failureCount;
  private String statusMessage;
  private LocalDateTime emailDate;
  private LocalDateTime plannedDeliveryTime;


  /**
   * The empty constructor is
   * needed for the construction of the pojo
   * with the Jackson library
   */
  @SuppressWarnings("unused")
  public MailingItemInputProps() {
  }


  @JsonProperty("messageId")
  public String getMessageId() {
    return messageId;
  }


  @SuppressWarnings("unused")
  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  @JsonProperty("statusMessage")
  public String getStatusMessage() {
    return statusMessage;
  }

  @SuppressWarnings("unused")
  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  @JsonProperty("emailServerReceiver")
  public String getEmailServerReceiver() {
    return this.emailServerReceiver;
  }

  @SuppressWarnings("unused")
  public void setEmailServerReceiver(String emailServerReceiver) {
    this.emailServerReceiver = emailServerReceiver;
  }
  @JsonProperty("emailServerSender")
  public String getEmailServerSender() {
    return emailServerSender;
  }
  @SuppressWarnings("unused")
  public void setEmailServerSender(String emailServerSender) {
    this.emailServerSender = emailServerSender;
  }

  @JsonProperty("mailingJobId")
  public MailingJob getMailingJob() {
    return this.mailingJobId;
  }

  @SuppressWarnings("unused")
  public void setMailingJob(MailingJob mailingJobId) {
    this.mailingJobId = mailingJobId;
  }

  @JsonProperty("failureCount")
  public Integer getFailureCount() {
    return this.failureCount;
  }

  @SuppressWarnings("unused")
  public void setFailureCount(Integer failureCount) {
    this.failureCount = failureCount;
  }

  @JsonProperty("status")
  public MailingItemStatus getStatus() {
    return this.status;
  }

  @SuppressWarnings("unused")
  public void setStatus(MailingItemStatus status) {
    this.status = status;
  }

  @JsonProperty("emailDate")
  public LocalDateTime getDeliveryDate() {
    return this.emailDate;
  }

  @SuppressWarnings("unused")
  public void setDeliveryDate(LocalDateTime emailDate) {
    this.emailDate = emailDate;
  }

  @JsonProperty("plannedDeliveryTime")
  public LocalDateTime getPlannedDeliveryTime() {
    return this.plannedDeliveryTime;
  }

  @SuppressWarnings("unused")
  public void setPlannedDeliveryTime(LocalDateTime plannedDeliveryTime) {
    this.plannedDeliveryTime = plannedDeliveryTime;
  }


}
