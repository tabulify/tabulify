package net.bytle.tower.eraldy.module.mailing.db.mailingrow;

import net.bytle.vertx.db.JdbcTableColumn;

public enum MailingRowCols implements JdbcTableColumn {

  REALM_ID("mailing_row_realm_id"),

  MAILING_ID("mailing_row_mailing_id"),
  MAILING_JOB_ID("mailing_row_mailing_job_id"),
  USER_ID("mailing_row_user_id"),
  STATUS_CODE("mailing_row_status_code"),
  STATUS_MESSAGE("mailing_row_status_message"),
  CREATION_TIME("mailing_row_creation_time"),
  MODIFICATION_TIME("mailing_row_modification_time"),
  PLANNED_DELIVERY_TIME("mailing_row_planned_delivery_time"),
  COUNT_FAILURE("mailing_row_failure_count");

  private final String colName;

  MailingRowCols(String colName) {
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
