package net.bytle.tower.eraldy.module.mailing.model;

import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.model.manual.Status;

/**
 * The status of the mailing
 * (ie SMTP status)
 */
public enum MailingItemStatus implements Status {


  PENDING(-2, 1, "Pending", "Not processed"),
  PROCESSING(-1, 1, "Processing", "In Process"),
  /**
   * Email send
   */
  OK(0, 1, "Send", "Email send"),

  ERROR(1, 2, "Fatal error", "Fatal error");



  private final int code;
  private final String name;
  private final String description;
  private final int order;

  MailingItemStatus(int code, int order, String name, String description) {
    this.order = order;
    this.code = code;
    this.name = name;
    this.description = description;
  }


  public static MailingItemStatus fromStatusCode(int statusCode) throws NotFoundException {
    for (MailingItemStatus value : values()) {
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

  public int getCode() {
    return this.code;
  }

  @Override
  public int getOrder() {
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
