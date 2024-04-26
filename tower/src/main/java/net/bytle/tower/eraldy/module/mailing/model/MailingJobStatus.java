package net.bytle.tower.eraldy.module.mailing.model;

import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.model.manual.Status;

/**
 * The status of the mailing job
 */
public enum MailingJobStatus implements Status {


  /**
   * Open (to process)
   */
  OPEN(-2,"Open"),
  /**
   * Running (in execution)
   */
  RUNNING(-1,"Running"),
  /**
   * Success
   */
  COMPLETED(0, "Success"),
  /**
   * A fatal error has occurred during execution
   * (our fault)
   */
  FATAL_ERROR(1, "FatalError");


  private final int statusCode;
  private final String statusName;

  MailingJobStatus(int statusCode, String statusName) {
    this.statusCode = statusCode;
    this.statusName = statusName;
  }

  /**
   * When the data comes from the database,
   * we sya that the data is good. If not, there is no default option that failing.
   */
  public static MailingJobStatus fromStatusCodeFailSafe(int statusCode) {
    try {
      return fromStatusCode(statusCode);
    } catch (NotFoundException e) {
      throw new RuntimeException("No Mailing status with the code (" + statusCode + ")");
    }

  }

  public static MailingJobStatus fromStatusCode(int statusCode) throws NotFoundException {
    for (MailingJobStatus value : values()) {
      if (value.statusCode == statusCode) {
        return value;
      }
    }
    throw new NotFoundException("The code (" + statusCode + ") is not a valid mailing job status");
  }

  @Override
  public String toString() {
    return statusCode + " (" + statusName + ")";
  }


  @Override
  public int getCode() {
    return this.statusCode;
  }

  @Override
  public int getOrder() {
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
