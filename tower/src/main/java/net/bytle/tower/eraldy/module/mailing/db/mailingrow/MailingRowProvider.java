package net.bytle.tower.eraldy.module.mailing.db.mailingrow;

import io.vertx.core.Future;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.module.mailing.model.Mailing;
import net.bytle.tower.eraldy.module.mailing.model.MailingJob;
import net.bytle.tower.eraldy.module.mailing.model.MailingRowStatus;
import net.bytle.vertx.db.*;

public class MailingRowProvider {

  private final EraldyApiApp apiApp;
    private final JdbcTable mailingRowTable;


  public MailingRowProvider(EraldyApiApp eraldyApiApp, JdbcSchema jdbcSchema) {
    this.apiApp = eraldyApiApp;
    this.mailingRowTable = JdbcTable.build(jdbcSchema, "realm_mailing_row")
      .build();
  }

  /**
   * We return the rows because we may get a lot
   * We use therefore the pointer to not load all of them in memory
   */
  public Future<JdbcRowSet> getRows(MailingJob mailingJob) {

    Mailing mailing = mailingJob.getMailing();

    JdbcSelect jdbcSelect = JdbcSelect.from(mailingRowTable)
      .addPredicate(
        JdbcSingleOperatorPredicate
          .builder()
          .setColumn(MailingRowCols.COUNT_FAILURE, this.apiApp.getMailingFlow().getMaxCountFailureOnRow())
          .setOperator(JdbcComparisonOperator.LESS_THAN)
          .setOrNull(true)
          .build()
      )
      .addPredicate(
        JdbcSingleOperatorPredicate
          .builder()
          .setColumn(MailingRowCols.STATUS_CODE, MailingRowStatus.OK.getCode())
          .setOperator(JdbcComparisonOperator.NOT_EQUAL)
          .setOrNull(true)
          .build()
      )
      .addEqualityPredicate(MailingRowCols.REALM_ID, mailing.getEmailRecipientList().getRealm().getLocalId())
      .addEqualityPredicate(MailingRowCols.MAILING_ID, mailing.getLocalId())
      .addLimit(mailingJob.getCountRowToExecute());

    return jdbcSelect.execute();
  }
}
