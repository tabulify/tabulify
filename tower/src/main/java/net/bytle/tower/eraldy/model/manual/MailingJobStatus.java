package net.bytle.tower.eraldy.model.manual;

/**
 * The status of the mailing job
 */
public enum MailingJobStatus implements Status{


  /**
   * Open (to process)
   */
  OPEN(-2,"open"),
  /**
   * Running (in execution)
   */
  RUNNING(-1,"running"),
  /**
   * Success
   */
  COMPLETED(0, "success"),
  /**
   * A fatal error has occurred during execution
   * (our fault)
   */
  FATAL_ERROR(1, "fatalError");


  private final int statusCode;
  private final String statusName;

  MailingJobStatus(int statusCode, String statusName) {
    this.statusCode = statusCode;
    this.statusName = statusName;
  }

  @Override
  public String toString() {
    return statusCode + " (" + statusName + ")";
  }


  @Override
  public Integer getCode() {
    return this.statusCode;
  }

  @Override
  public Integer getOrder() {
    return this.statusCode;
  }

  @Override
  public String getName() {
    return this.statusName;
  }

  @Override
  public String getDescription() {
    return this.statusName;
  }

}
