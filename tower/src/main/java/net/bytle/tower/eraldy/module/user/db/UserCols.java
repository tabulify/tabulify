package net.bytle.tower.eraldy.module.user.db;

import net.bytle.vertx.db.JdbcColumn;

public enum UserCols implements JdbcColumn {


  ID("user_id"),
  REALM_ID("user_realm_id"),
  /**
   * The handle is the email address
   */
  EMAIL_ADDRESS("user_email_address"),
  MODIFICATION_IME("user_modification_time"),
  DATA("user_data"),
  CREATION_TIME("user_creation_time"),
  PASSWORD("user_password"),

  BIO("user_bio"),
  TITLE("user_title"),
  TIME_ZONE("user_time_zone"),
  FAMILY_NAME("user_family_name"),
  GIVEN_NAME("user_given_name"),
  LOCATION("user_location"),
  STATUS_MESSAGE("user_status_message"),
  STATUS_CODE("user_status_code"),
  AVATAR("user_avatar"),
  WEBSITE("user_website"),
  LAST_ACTIVE_TIME("user_last_active_time");

  private final String columnName;

  UserCols(String columnName) {
    this.columnName = columnName;
  }

  @Override
  public String getColumnName() {
    return this.columnName;
  }
}
