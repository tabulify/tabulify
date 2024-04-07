package net.bytle.tower.eraldy.api.implementer.flow.mailing;

/**
 * The status of the mailing
 */
public enum MailingStatus {


  /**
   * The mailing is open, no mailing has been done
   */
  COMPLETED(0, "open"),
  /**
   * The mailing is closed, no action can be taken
   */
  FATAL_ERROR(1, "closed");


  private final int statusCode;
  private final String statusName;

  MailingStatus(int statusCode, String statusName) {
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
