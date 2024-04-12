package net.bytle.tower.eraldy.model.manual;

import net.bytle.exception.NotFoundException;

/**
 * The status of the mailing
 */
public enum MailingRowStatus implements Status {





  /**
   * No email to be sent anymore
   */
  SEND(0, 3, "Send", "Email send"),

  ERROR(1, 6, "Fatal error", "Fatal error");





  private final int code;
  private final String name;
  private final String description;
  private final int order;

  MailingRowStatus(int code, int order, String name, String description) {
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
  public static MailingRowStatus fromStatusCodeFailSafe(int statusCode) {
    try {
      return fromStatusCode(statusCode);
    } catch (NotFoundException e) {
      throw new RuntimeException("No Mailing status with the code (" + statusCode + ")");
    }

  }

  public static MailingRowStatus fromStatusCode(int statusCode) throws NotFoundException {
    for (MailingRowStatus value : values()) {
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
