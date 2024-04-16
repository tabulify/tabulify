package net.bytle.tower.eraldy.module.mailing.db.mailing;

import net.bytle.tower.eraldy.module.list.db.ListProvider;
import net.bytle.tower.eraldy.objectProvider.OrganizationProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.vertx.db.JdbcSchemaManager;
import net.bytle.vertx.db.JdbcTableColumn;

import static net.bytle.tower.eraldy.module.mailing.db.mailing.MailingProvider.MAILING_PREFIX;
import static net.bytle.vertx.db.JdbcSchemaManager.COLUMN_PART_SEP;

public enum MailingCols implements JdbcTableColumn {

  MODIFICATION_TIME(MAILING_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX),
  CREATION_TIME (MAILING_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX),
  LAST_EXECUTION_TIME (MAILING_PREFIX + COLUMN_PART_SEP + "job_last_execution_time"),
  NEXT_EXECUTION_TIME( MAILING_PREFIX + COLUMN_PART_SEP + "job_next_execution_time"),
  ITEM_COUNT( MAILING_PREFIX + COLUMN_PART_SEP + "item_count"),
  ITEM_SUCCESS_COUNT(MAILING_PREFIX + COLUMN_PART_SEP + "item_success_count"),
  ITEM_EXECUTION_COUNT(MAILING_PREFIX + COLUMN_PART_SEP + "item_execution_count"),
  ORGA_ID (MAILING_PREFIX + COLUMN_PART_SEP + OrganizationProvider.ORGA_ID_COLUMN),

  EMAIL_AUTHOR_USER_ID (MAILING_PREFIX + COLUMN_PART_SEP + "email_author" + COLUMN_PART_SEP + UserProvider.ID_COLUMN),
  ID(MAILING_PREFIX + COLUMN_PART_SEP + "id"),
  NAME(MAILING_PREFIX + COLUMN_PART_SEP + "name"),
  REALM_ID( MAILING_PREFIX + COLUMN_PART_SEP + "realm_id"),
  STATUS_CODE( MAILING_PREFIX + COLUMN_PART_SEP + "status"),
  EMAIL_SUBJECT (MAILING_PREFIX + COLUMN_PART_SEP + "email_subject"),
  EMAIL_PREVIEW (MAILING_PREFIX + COLUMN_PART_SEP + "email_preview"),
  EMAIL_BODY ( MAILING_PREFIX + COLUMN_PART_SEP + "email_body"),

  EMAIL_LANGUAGE ( MAILING_PREFIX + COLUMN_PART_SEP + "email_language"),
  EMAIL_RCPT_LIST_ID(MAILING_PREFIX + COLUMN_PART_SEP + "email_rcpt" + COLUMN_PART_SEP + ListProvider.LIST_ID_COLUMN);

  private final String colName;

  MailingCols(String colName) {
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
