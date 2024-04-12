package net.bytle.tower.eraldy.module.mailing.model;

import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.model.manual.Status;

/**
 * The status of the mailing
 */
public enum MailingStatus implements Status {


  /**
   * Open
   * (0 because this is the first number, easy to remember to reset the mailing flow)
   */
  OPEN(0, 1, "Open", "The mailing is being defined, no email has been sent"),

  /**
   * Processing the request
   */
  PROCESSING(1, 2, "Processing", "The mailing request has been created. At least, one job has been executed or is executing."),


  /**
   * No email to be sent anymore
   */
  COMPLETED(4, 3, "Completed", "No email to sent anymore"),

  /**
   * Cancel
   */
  CANCELED(5, 4, "Canceled", "No action anymore"),
  /**
   * Pause
   */
  PAUSED(6, 5, "Paused", "Paused: no job will send emails"),
  /**
   * Fatal Error
   */
  ERROR(10, 6, "Fatal error", "Fatal error");


  private final int code;
  private final String name;
  private final String description;
  private final int order;

  MailingStatus(int code, int order, String name, String description) {
    this.order = order;
    this.code = code;
    this.name = name;
    this.description = description;
  }


  /**
   *
   * @param statusCode - the status code
   * @throws RuntimeException will fail at runtime if the status code is unknown
   *                   Be sure to master the value
   */
  public static MailingStatus fromStatusCodeFailSafe(int statusCode) {
    try {
      return fromStatusCode(statusCode);
    } catch (NotFoundException e) {
      throw new RuntimeException("No Mailing status with the code (" + statusCode + ")");
    }

  }

  public static MailingStatus fromStatusCode(int statusCode) throws NotFoundException {
    for (MailingStatus value : values()) {
      if (value.code == statusCode) {
        return value;
      }
    }
    throw new NotFoundException("The code (" + statusCode + ") is not a valid mailing status");
  }

  @Override
  public String toString() {
    return code + " (" + name + ")";
  }

  public Integer getCode() {
    return this.code;
  }

  @Override
  public Integer getOrder() {
    return this.order;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getDescription() {
    return this.description;
  }


}
