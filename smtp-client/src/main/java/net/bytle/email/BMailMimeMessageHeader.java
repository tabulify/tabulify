package net.bytle.email;

public enum BMailMimeMessageHeader {

  /**
   * Trace info: The date when the message was received by the SMTP server
   * and by who.
   * <a href="https://www.rfc-editor.org/rfc/rfc822.html#section-4.3.2">...</a>
   */
  RECEIVED("Received"),
  /**
   * Trace info: Where to send back the email in case of problem.
   * <p>
   * Diff with Reply-To:
   * The "Reply-To" field is added  by  the  originator  and
   * serves  to  direct  replies,  whereas the "Return-Path"
   * field is used to identify a path back to  the  originator
   * in case of bounce
   * <a href="https://www.rfc-editor.org/rfc/rfc822.html#section-4.3.1">...</a>
   */
  RETURN_PATH("Return-Path"),
  /**
   * The "Reply-To" field is added  by  the  originator  and
   * serves  to  direct  replies,  whereas the "Return-Path"
   * field is used to identify a path back to  the  originator
   * in case of bounce
   */
  REPLY_TO("Reply-To");



  private final String name;

  BMailMimeMessageHeader(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
