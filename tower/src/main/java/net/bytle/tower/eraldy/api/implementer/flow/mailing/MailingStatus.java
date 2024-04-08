package net.bytle.tower.eraldy.api.implementer.flow.mailing;

import net.bytle.tower.eraldy.model.manual.Status;

/**
 * The status of the mailing
 */
public enum MailingStatus implements Status {


  /**
   * Open
   */
  OPEN(-2, "Open", "The mailing is being defined, no email has been sent"),

  /**
   * Scheduled
   */
  SCHEDULED(-1, "Scheduled", "The mailing is scheduled. Email may have been sent."),

  /**
   * No email to be sent anymore
   */
  COMPLETED(0, "Completed", "No email to sent anymore"),

  /**
   * Fatal Error
   */
  ERROR(1, "Fatal error", "Fatal error");


  private final int statusCode;
  private final String statusName;
  private final String statusDescription;

  MailingStatus(int statusCode, String statusName, String description) {
    this.statusCode = statusCode;
    this.statusName = statusName;
    this.statusDescription = description;
  }

  /**
   *
   * @param statusCode - the status code
   * @throws RuntimeException will fail at runtime if the status code is unknown
   *                   Be sure to master the value
   */
  public static MailingStatus fromStatusCodeFailSafe(int statusCode) {
    for (MailingStatus value : values()) {
      if (value.statusCode == statusCode) {
        return value;
      }
    }
    throw new RuntimeException("No Mailing status with the code (" + statusCode + ")");
  }

  @Override
  public String toString() {
    return statusCode + " (" + statusName + ")";
  }

  public Integer getCode() {
    return this.statusCode;
  }

  @Override
  public String getName() {
    return this.statusName;
  }

  @Override
  public String getDescription() {
    return this.statusDescription;
  }


}
