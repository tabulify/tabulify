package net.bytle.tower.eraldy.api.implementer.flow.mailing;

/**
 * SMTP status
 */
public enum MailingJobEmailStatus {

  /**
   * Open
   */
  OPEN(-2, "open"),
  /**
   * Running
   */
  RUNNING(-1, "running"),
  /**
   * The SMTP transfer completed without any error
   */
  COMPLETED(0, "complete"),
  /**
   * A fatal error has occurred during
   */
  FATAL_ERROR(1, "fatalError");


  private final int statusCode;
  private final String statusName;

  MailingJobEmailStatus(int statusCode, String statusName) {
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
