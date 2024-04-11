package net.bytle.tower.eraldy.objectProvider;

import static net.bytle.vertx.JdbcSchemaManager.COLUMN_PART_SEP;

public class MailingRowProvider {
  public static final String FULL_TABLE = "cs_jobs.realm_mailing_row";

  private static final String MAILING_ROW_PREFIX = "mailing_row";
  public static final String MAILING_ROW_REALM_COLUMN = MAILING_ROW_PREFIX + COLUMN_PART_SEP + RealmProvider.REALM_ID_COLUMN;
  public static final String MAILING_ROW_MAILING_COLUMN = MAILING_ROW_PREFIX + COLUMN_PART_SEP + "mailing_id";


}
