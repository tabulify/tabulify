package net.bytle.email;

public enum BMailStartTls {

  NONE,
  /**
   * Enabled but not required secure connection
   */
  ENABLE,
  /**
   * Secure connection required
   */
  REQUIRE;

  /**
   * Utility function to return the mail property value
   * mail.smtpt.startls.enable
   */
  public boolean getEnableStartTls() {
    return this == ENABLE || this == REQUIRE;
  }

  /**
   * Utility function to return the mail property value
   * mail.smtpt.startls.require
   */
  public boolean getRequireStartTls() {
    return this == REQUIRE;
  }
}
