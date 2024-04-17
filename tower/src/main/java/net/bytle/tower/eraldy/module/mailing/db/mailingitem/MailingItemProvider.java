package net.bytle.tower.eraldy.module.mailing.db.mailingitem;

import io.vertx.core.Future;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.ListUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.mailing.model.Mailing;
import net.bytle.tower.eraldy.module.mailing.model.MailingItem;
import net.bytle.tower.eraldy.module.mailing.model.MailingItemStatus;
import net.bytle.tower.eraldy.module.mailing.model.MailingJob;
import net.bytle.tower.eraldy.objectProvider.UserCols;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.db.*;

import java.util.*;

public class MailingItemProvider {

  private final EraldyApiApp apiApp;
  private final JdbcTable mailingItemTable;


  /**
   * mat and not mai because mai is already taken by mailing
   */
  private final String GUID_PREFIX = "mat";


  public MailingItemProvider(EraldyApiApp eraldyApiApp, JdbcSchema jdbcSchema) {
    this.apiApp = eraldyApiApp;
    Map<JdbcTableColumn, JdbcTableColumn> mailingUserForeignsKeys = new HashMap<>();
    mailingUserForeignsKeys.put(MailingItemCols.REALM_ID, UserCols.REALM_ID);
    mailingUserForeignsKeys.put(MailingItemCols.USER_ID, UserCols.ID);
    this.mailingItemTable = JdbcTable.build(jdbcSchema, "realm_mailing_item")
      .addForeignKeyColumns(this.apiApp.getUserProvider().getUserTable(), mailingUserForeignsKeys)
      .build();
  }

  /**
   * We return the rows because we may get a lot
   * We use therefore the pointer to not load all of them in memory
   */
  public Future<JdbcRowSet> getItemsForJobExecution(MailingJob mailingJob) {

    Mailing mailing = mailingJob.getMailing();

    JdbcSelect jdbcSelect = JdbcSelect.from(mailingItemTable)
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
          .setColumn(MailingItemCols.STATUS_CODE, MailingItemStatus.OK.getCode())
          .setOperator(JdbcComparisonOperator.NOT_EQUAL)
          .setOrNull(true)
          .build()
      )
      .addEqualityPredicate(MailingItemCols.REALM_ID, mailing.getEmailRecipientList().getRealm().getLocalId())
      .addEqualityPredicate(MailingItemCols.MAILING_ID, mailing.getLocalId())
      .addLimit(mailingJob.getItemToExecuteCount());

    return jdbcSelect.execute();
  }

  public Future<List<MailingItem>> getItemsForGraphQL(Mailing mailing, JdbcPagination pagination) {
    JdbcTable userTable = this.apiApp.getUserProvider().getUserTable();
    return JdbcPaginatedSelect.from(mailingItemTable)
      .addEqualityPredicate(mailingItemTable, MailingItemCols.REALM_ID, mailing.getRealm().getLocalId())
      .addEqualityPredicate(mailingItemTable, MailingItemCols.MAILING_ID, mailing.getLocalId())
      .setSearchColumn(userTable, UserCols.EMAIL_ADDRESS)
      .addExtraSelectColumn(userTable, UserCols.EMAIL_ADDRESS)
      .addEqualityPredicate(userTable, UserCols.REALM_ID, mailing.getRealm().getLocalId())
      .addOrderBy(userTable, UserCols.EMAIL_ADDRESS, JdbcSort.ASC)
      .setPagination(pagination)
      .execute(jdbcRowSet -> {
        List<MailingItem> mailingItems = new ArrayList<>();
        for (JdbcRow jdbcRow : jdbcRowSet) {
          MailingItem mailingItem = this.buildingMailingItemFromRow(jdbcRow, mailing);
          // Add address email for front end
          User user = mailingItem.getListUser().getUser();
          user.setEmailAddress(jdbcRow.getString(UserCols.EMAIL_ADDRESS));
          mailingItems.add(mailingItem);
        }
        return Future.succeededFuture(mailingItems);
      });

  }

  /**
   *
   * @param jdbcRow - the row
   * @param mailing - the mailing is mandatory to build the list user
   */
  private MailingItem buildingMailingItemFromRow(JdbcRow jdbcRow, Mailing mailing) {
    MailingItem mailingItem = new MailingItem();

    /**
     * Realm
     */
    Realm realm = new Realm();
    realm.setLocalId(jdbcRow.getLong(MailingItemCols.REALM_ID));

    /**
     * Mailing
     */

      Long mailingId = jdbcRow.getLong(MailingItemCols.MAILING_ID);
    if(!Objects.equals(mailingId, mailing.getLocalId())){
      throw new InternalException("Bad mailing id");
    }
    mailingItem.setMailing(mailing);


    /**
     * User
     */
    User user = new User();
    user.setLocalId(jdbcRow.getLong(MailingItemCols.USER_ID));
    user.setRealm(realm);
    this.apiApp.getUserProvider().updateGuid(user);

    /**
     * List User
     */
    ListUser listUser = new ListUser();
    listUser.setList(mailing.getEmailRecipientList());
    listUser.setUser(user);
    this.apiApp.getListUserProvider().updateGuid(listUser);
    mailingItem.setListUser(listUser);

    /**
     * Guid
     */
    this.updateGuid(mailingItem);

    /**
     * Status
     */
    Integer statusCode = jdbcRow.getInteger(MailingItemCols.STATUS_CODE);
    try {
      mailingItem.setStatus(MailingItemStatus.fromStatusCode(statusCode));
    } catch (NotFoundException e) {
      throw new InternalException("The mailing item status code (" + statusCode + ") is unknown for the mailing item (" + mailingItem + ")");
    }
    String statusMessage = jdbcRow.getString(MailingItemCols.STATUS_MESSAGE);
    mailingItem.setStatusMessage(statusMessage);

    /**
     * Mailing Job
     */
    Long mailingJobId = jdbcRow.getLong(MailingItemCols.MAILING_JOB_ID);
    if (mailingJobId != null) {
      MailingJob mailingJob = new MailingJob();
      mailingJob.setLocalId(mailingJobId);
      mailingJob.setMailing(mailing);
      this.apiApp.getMailingJobProvider().updateGuid(mailingJob);
      mailingItem.setMailingJob(mailingJob);
    }

    /**
     * Time
     */
    mailingItem.setCreationTime(jdbcRow.getLocalDateTime(MailingItemCols.CREATION_TIME));
    mailingItem.setModificationTime(jdbcRow.getLocalDateTime(MailingItemCols.MODIFICATION_TIME));

    /**
     * Processing attribute
     */
    mailingItem.setFailureCount(jdbcRow.getInteger(MailingItemCols.COUNT_FAILURE, 0));
    mailingItem.setEmailDate(jdbcRow.getLocalDateTime(MailingItemCols.EMAIL_DATE));

    return mailingItem;
  }

  private void updateGuid(MailingItem mailingItem) {

    Guid guid = this.apiApp.createGuidStringFromRealmAndTwoObjectId(
      GUID_PREFIX,
      mailingItem.getMailing().getRealm().getLocalId(),
      mailingItem.getMailing().getLocalId(),
      mailingItem.getListUser().getUser().getLocalId()
    );
    mailingItem.setGuid(guid.toString());

  }
}
