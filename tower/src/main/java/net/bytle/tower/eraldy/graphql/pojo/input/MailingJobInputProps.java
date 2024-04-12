package net.bytle.tower.eraldy.graphql.pojo.input;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.manual.MailingJobStatus;

import java.time.LocalDateTime;

/**
 * Mailing Job Input Props
 * for the creation and or modification of a mailing job
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MailingJobInputProps {


  protected MailingJobStatus status;
  private String statusMessage;
  private LocalDateTime endTime;


  /**
   * The empty constructor is
   * needed for the construction of the pojo
   * with the Jackson library
   */
  @SuppressWarnings("unused")
  public MailingJobInputProps() {
  }


  @JsonProperty("statusMessage")
  public String getStatusMessage() {
    return statusMessage;
  }


  @SuppressWarnings("unused")
  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }


  @JsonProperty("status")
  public MailingJobStatus getStatus() {
    return this.status;
  }

  @SuppressWarnings("unused")
  public void setStatus(MailingJobStatus status) {
    this.status = status;
  }

  @JsonProperty("endTime")
  public LocalDateTime getEndTime() {
    return this.endTime;
  }

  @SuppressWarnings("unused")
  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }


}
