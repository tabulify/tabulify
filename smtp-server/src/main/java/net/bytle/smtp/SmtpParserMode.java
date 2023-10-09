package net.bytle.smtp;

public enum SmtpParserMode {

  /**
   * This is the default mode
   * where the records are created line by line
   */
  LINE,
  /**
   * This is the {@link net.bytle.smtp.command.SmtpBdatCommandHandler}
   * Bdat mode where the amount of data is fixed
   */
  FIXED
}
