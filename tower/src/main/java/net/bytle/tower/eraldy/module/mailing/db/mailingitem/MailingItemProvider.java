package net.bytle.tower.eraldy.module.mailing.db.mailingitem;

import io.vertx.core.Future;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.mailing.model.Mailing;
import net.bytle.tower.eraldy.module.mailing.model.MailingItem;
import net.bytle.tower.eraldy.module.mailing.model.MailingJob;
import net.bytle.tower.eraldy.module.mailing.model.MailingRowStatus;
import net.bytle.tower.eraldy.objectProvider.UserCols;
import net.bytle.vertx.db.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MailingItemProvider {

  private final EraldyApiApp apiApp;
  private final JdbcTable mailingRowTable;


  public MailingItemProvider(EraldyApiApp eraldyApiApp, JdbcSchema jdbcSchema) {
    this.apiApp = eraldyApiApp;
    Map<JdbcTableColumn, JdbcTableColumn> mailingUserForeignsKeys = new HashMap<>();
    mailingUserForeignsKeys.put(MailingItemCols.REALM_ID, UserCols.REALM_ID);
    mailingUserForeignsKeys.put(MailingItemCols.USER_ID, UserCols.ID);
    this.mailingRowTable = JdbcTable.build(jdbcSchema, "realm_mailing_item")
      .addForeignKeyColumns(this.apiApp.getUserProvider().getUserTable(), mailingUserForeignsKeys)
      .build();
  }

  /**
   * We return the rows because we may get a lot
   * We use therefore the pointer to not load all of them in memory
   */
  public Future<JdbcRowSet> getItemsForJobExecution(MailingJob mailingJob) {

    Mailing mailing = mailingJob.getMailing();

    JdbcSelect jdbcSelect = JdbcSelect.from(mailingRowTable)
      .addPredicate(
        JdbcSingleOperatorPredicate
          .builder()
          .setColumn(MailingItemCols.COUNT_FAILURE, this.apiApp.getMailingFlow().getMaxCountFailureOnRow())
          .setOperator(JdbcComparisonOperator.LESS_THAN)
          .setOrNull(true)
          .build()
      )
      .addPredicate(
        JdbcSingleOperatorPredicate
          .builder()
          .setColumn(MailingItemCols.STATUS_CODE, MailingRowStatus.OK.getCode())
          .setOperator(JdbcComparisonOperator.NOT_EQUAL)
          .setOrNull(true)
          .build()
      )
      .addEqualityPredicate(MailingItemCols.REALM_ID, mailing.getEmailRecipientList().getRealm().getLocalId())
      .addEqualityPredicate(MailingItemCols.MAILING_ID, mailing.getLocalId())
      .addLimit(mailingJob.getCountRowToExecute());

    return jdbcSelect.execute();
  }

  public Future<List<MailingItem>> getItemsForGraphQL(Mailing mailing, JdbcPagination pagination) {
    JdbcTable userTable = this.apiApp.getUserProvider().getUserTable();
    return JdbcPaginatedSelect.from(mailingRowTable)
      .addEqualityPredicate(mailingRowTable, MailingItemCols.REALM_ID, mailing.getRealm().getLocalId())
      .addEqualityPredicate(mailingRowTable, MailingItemCols.MAILING_ID, mailing.getLocalId())
      .setSearchColumn(userTable, UserCols.EMAIL_ADDRESS)
      .addExtraSelectColumn(userTable, UserCols.EMAIL_ADDRESS)
      .addEqualityPredicate(userTable, UserCols.REALM_ID, mailing.getRealm().getLocalId())
      .addOrderBy(userTable, UserCols.EMAIL_ADDRESS, JdbcSort.ASC)
      .setPagination(pagination)
      .execute(jdbcRowSet -> {
        List<MailingItem> mailingItems = new ArrayList<>();
        for (JdbcRow jdbcRow : jdbcRowSet) {
          MailingItem mailingItem = this.buildingMailingItemFromRow(jdbcRow);
          mailingItem.setMailing(mailing);
          // Add address
          User user = mailingItem.getUser();
          user.setEmailAddress(jdbcRow.getString(UserCols.EMAIL_ADDRESS));
        }
        return Future.succeededFuture(mailingItems);
      });

  }

  private MailingItem buildingMailingItemFromRow(JdbcRow jdbcRow) {
    MailingItem mailingItem = new MailingItem();

    /**
     * Realm
     */
    Realm realm = new Realm();
    realm.setLocalId(jdbcRow.getLong(MailingItemCols.REALM_ID));

    /**
     * Mailing
     */
    Mailing mailing = new Mailing();
    Long mailingId = jdbcRow.getLong(MailingItemCols.MAILING_ID);
    mailing.setLocalId(mailingId);
    mailing.setRealm(realm);
    mailingItem.setMailing(mailing);

    /**
     * User
     */
    User user = new User();
    user.setLocalId(jdbcRow.getLong(MailingItemCols.USER_ID));
    user.setRealm(realm);
    mailingItem.setUser(user);

    /**
     * Status
     */
    Integer statusCode = jdbcRow.getInteger(MailingItemCols.STATUS_CODE);
    try {
      mailingItem.setStatus(MailingRowStatus.fromStatusCode(statusCode));
    } catch (NotFoundException e) {
      throw new InternalException("The mailing item status code (" + statusCode + ") is unknown for the mailing item (" + mailingItem + ")");
    }

    return mailingItem;
  }
}
