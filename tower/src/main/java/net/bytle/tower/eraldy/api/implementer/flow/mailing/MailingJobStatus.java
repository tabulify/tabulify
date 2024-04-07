package net.bytle.tower.eraldy.api.implementer.flow.mailing;

/**
 * The status of the mailing job
 */
public enum MailingJobStatus {


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


  public int getStatusCode() {
    return this.statusCode;
  }


}
