package net.bytle.tower.eraldy.module.mailing.db.mailingitem;

import net.bytle.vertx.db.JdbcTableColumn;

public enum MailingItemCols implements JdbcTableColumn {

  REALM_ID("mailing_item_realm_id"),

  MAILING_ID("mailing_item_mailing_id"),
  MAILING_JOB_ID("mailing_item_mailing_job_id"),
  USER_ID("mailing_item_user_id"),
  STATUS_CODE("mailing_item_status_code"),
  STATUS_MESSAGE("mailing_item_status_message"),
  CREATION_TIME("mailing_item_creation_time"),
  MODIFICATION_TIME("mailing_item_modification_time"),
  PLANNED_DELIVERY_TIME("mailing_item_planned_delivery_time"),
  FAILURE_COUNT("mailing_item_failure_count"),
  DELIVERY_DATE("mailing_item_email_date"),
  EMAIL_MESSAGE_ID("mailing_item_email_message_id");

  private final String colName;

  MailingItemCols(String colName) {
    this.colName = colName;
  }

  @Override
  public String toString() {
    return colName;
  }

  @Override
  public String getColumnName() {
    return colName;
  }
}
