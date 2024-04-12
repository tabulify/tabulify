package net.bytle.tower.eraldy.module.mailing.objectProvider;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.mailing.model.Mailing;
import net.bytle.tower.eraldy.module.mailing.model.MailingJob;
import net.bytle.tower.eraldy.module.mailing.model.MailingRowStatus;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.vertx.JdbcClient;

import static net.bytle.vertx.JdbcSchemaManager.COLUMN_PART_SEP;

public class MailingRowProvider {
  public static final String FULL_TABLE = "cs_jobs.realm_mailing_row";

  private static final String MAILING_ROW_PREFIX = "mailing_row";
  public static final String MAILING_ROW_REALM_COLUMN = MAILING_ROW_PREFIX + COLUMN_PART_SEP + RealmProvider.REALM_ID_COLUMN;
  public static final String MAILING_ROW_MAILING_COLUMN = MAILING_ROW_PREFIX + COLUMN_PART_SEP + "mailing_id";
  public static final String MAILING_ROW_STATUS_CODE_COLUMN = MAILING_ROW_PREFIX + COLUMN_PART_SEP + "status_code";
  public static final String MAILING_ROW_COUNT_FAILURE_COLUMN = MAILING_ROW_PREFIX + COLUMN_PART_SEP + "count_failure";
  private final EraldyApiApp apiApp;
  private final JdbcClient jdbcClient;


  public MailingRowProvider(EraldyApiApp eraldyApiApp) {
    this.apiApp = eraldyApiApp;
    this.jdbcClient = this.apiApp.getHttpServer().getServer().getPostgresClient();
  }

  /**
   * We return the rows because we may get a lot
   * We use therefore the pointer to not load all of them in memory
   */
  public Future<RowSet<Row>> getRows(MailingJob mailingJob) {

    Mailing mailing = mailingJob.getMailing();
    final String sql = "select * from " + FULL_TABLE
      + " where "
      + MAILING_ROW_COUNT_FAILURE_COLUMN + " < $1\n"
      + "and " + MAILING_ROW_STATUS_CODE_COLUMN + " != $2\n"
      + "and " + MAILING_ROW_REALM_COLUMN + " = $3\n"
      + "and " + MAILING_ROW_MAILING_COLUMN + " = $4\n"
      + "LIMIT $5";
    return jdbcClient
      .getPool()
      .preparedQuery(sql)
      .execute(Tuple.of(
        this.apiApp.getMailingFlow().getMaxCountFailureOnRow(),
        MailingRowStatus.OK.getCode(),
        mailing.getEmailRecipientList().getRealm().getLocalId(),
        mailing.getLocalId(),
        mailingJob.getCountRowToExecute()
      ))
      .recover(err -> Future.failedFuture(new InternalException("The getMailingRows SQL returns an error. Error: " + err.getMessage() + "\nSQL:" + sql)));
  }
}
