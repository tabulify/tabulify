package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * This object represents the status of a job that imports users for a list.
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListImportJobStatus  implements net.bytle.vertx.TowerCompositeFutureListener {


  protected String jobId;

  protected Integer statusCode;

  protected String statusMessage;

  protected Integer listUserActionCode;

  protected Integer userActionCode;

  protected String uploadedFileName;

  protected Integer countTotal;

  protected Integer countComplete;

  protected Integer countSuccess;

  protected Integer countFailure;

  protected LocalDateTime creationTime;

  protected LocalDateTime startTime;

  protected LocalDateTime endTime;
  private String listGuid;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ListImportJobStatus () {
  }

  /**
  * @return jobId The job id
  */
  @JsonProperty("jobId")
  public String getJobId() {
    return jobId;
  }

  /**
  * @param jobId The job id
  */
  @SuppressWarnings("unused")
  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  /**
  * @return statusCode The status code: -2: queued, -1: processing, 0: done, 1: fatal error, 2: quota reached
  */
  @JsonProperty("statusCode")
  public Integer getStatusCode() {
    return statusCode;
  }

  /**
  * @param statusCode The status code: -2: queued, -1: processing, 0: done, 1: fatal error, 2: quota reached
  */
  @SuppressWarnings("unused")
  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  /**
  * @return statusMessage A description of any fatal error if any
  */
  @JsonProperty("statusMessage")
  public String getStatusMessage() {
    return statusMessage;
  }

  /**
  * @param statusMessage A description of any fatal error if any
  */
  @SuppressWarnings("unused")
  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  /**
  * @return listUserActionCode The action on the list user: 0: add (in), 1: delete (out)
  */
  @JsonProperty("listUserActionCode")
  public Integer getListUserActionCode() {
    return listUserActionCode;
  }

  /**
  * @param listUserActionCode The action on the list user: 0: add (in), 1: delete (out)
  */
  @SuppressWarnings("unused")
  public void setListUserActionCode(Integer listUserActionCode) {
    this.listUserActionCode = listUserActionCode;
  }

  /**
  * @return userActionCode The action on the user: 0: no update (create only), 1: update
  */
  @JsonProperty("userActionCode")
  public Integer getUserActionCode() {
    return userActionCode;
  }

  /**
  * @param userActionCode The action on the user: 0: no update (create only), 1: update
  */
  @SuppressWarnings("unused")
  public void setUserActionCode(Integer userActionCode) {
    this.userActionCode = userActionCode;
  }

  /**
  * @return uploadedFileName The original file name
  */
  @JsonProperty("uploadedFileName")
  public String getUploadedFileName() {
    return uploadedFileName;
  }

  /**
  * @param uploadedFileName The original file name
  */
  @SuppressWarnings("unused")
  public void setUploadedFileName(String uploadedFileName) {
    this.uploadedFileName = uploadedFileName;
  }

  /**
  * @return countTotal The number of rows to process
  */
  @JsonProperty("countTotal")
  public Integer getCountTotal() {
    return countTotal;
  }

  /**
  * @param countTotal The number of rows to process
  */
  @SuppressWarnings("unused")
  public void setCountTotal(Integer countTotal) {
    this.countTotal = countTotal;
  }

  /**
  * @return countComplete The number of rows processed
  */
  @JsonProperty("countComplete")
  public Integer getCountComplete() {
    return countComplete;
  }

  /**
  * @param countComplete The number of rows processed
  */
  @SuppressWarnings("unused")
  public void setCountComplete(Integer countComplete) {
    this.countComplete = countComplete;
  }

  /**
  * @return countSuccess The number of rows processed without a fatal failure
  */
  @JsonProperty("countSuccess")
  public Integer getCountSuccess() {
    return countSuccess;
  }

  /**
  * @param countSuccess The number of rows processed without a fatal failure
  */
  @SuppressWarnings("unused")
  public void setCountSuccess(Integer countSuccess) {
    this.countSuccess = countSuccess;
  }

  /**
  * @return countFailure The number of rows processed with a fatal failure
  */
  @JsonProperty("countFailure")
  public Integer getCountFailure() {
    return countFailure;
  }

  /**
  * @param countFailure The number of rows processed with a fatal failure
  */
  @SuppressWarnings("unused")
  public void setCountFailure(Integer countFailure) {
    this.countFailure = countFailure;
  }

  /**
  * @return creationTime
  */
  @JsonProperty("creationTime")
  public LocalDateTime getCreationTime() {
    return creationTime;
  }

  /**
  * @param creationTime Set creationTime
  */
  @SuppressWarnings("unused")
  public void setCreationTime(LocalDateTime creationTime) {
    this.creationTime = creationTime;
  }

  /**
  * @return startTime
  */
  @JsonProperty("startTime")
  public LocalDateTime getStartTime() {
    return startTime;
  }

  /**
  * @param startTime Set startTime
  */
  @SuppressWarnings("unused")
  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  /**
  * @return endTime
  */
  @JsonProperty("endTime")
  public LocalDateTime getEndTime() {
    return endTime;
  }

  /**
  * @param endTime Set endTime
  */
  @SuppressWarnings("unused")
  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListImportJobStatus listImportJobStatus = (ListImportJobStatus) o;
    return
            Objects.equals(jobId, listImportJobStatus.jobId);

  }

  @Override
  public int hashCode() {
    return Objects.hash(jobId);
  }

  @Override
  public String toString() {
    return jobId;
  }

  /**
   * @return userActionCode The action on the user: 0: no update (create only), 1: update
   */
  @JsonProperty("listGuid")
  public String getListGuid() {
    return listGuid;
  }
  public ListImportJobStatus setListGuid(String listGuid) {
    this.listGuid = listGuid;
    return this;
  }

}
