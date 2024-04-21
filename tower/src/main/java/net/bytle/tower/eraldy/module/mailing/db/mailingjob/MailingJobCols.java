package net.bytle.tower.eraldy.module.mailing.db.mailingjob;

import net.bytle.vertx.db.JdbcColumn;

public enum MailingJobCols implements JdbcColumn {

  ID("mailing_job_id"),
  REALM_ID("mailing_job_realm_id"),

  MAILING_ID("mailing_job_mailing_id"),
  STATUS_CODE("mailing_job_status_code"),
  STATUS_MESSAGE("mailing_job_status_message"),
  START_TIME("mailing_job_start_time"),
  END_TIME("mailing_job_end_time"),
  ITEM_TO_EXECUTE_COUNT("mailing_job_item_to_execute_count"),
  ITEM_SUCCESS_COUNT("mailing_job_item_success_count"),
  ITEM_EXECUTION_COUNT("mailing_job_item_execution_count");

  private final String colName;

  MailingJobCols(String colName) {
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
