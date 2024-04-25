package net.bytle.tower.eraldy.module.mailing.db.mailingitem;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.model.openapi.ListUser;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.mailing.inputs.MailingItemInputProps;
import net.bytle.tower.eraldy.module.mailing.model.Mailing;
import net.bytle.tower.eraldy.module.mailing.model.MailingItem;
import net.bytle.tower.eraldy.module.mailing.model.MailingItemStatus;
import net.bytle.tower.eraldy.module.mailing.model.MailingJob;
import net.bytle.tower.eraldy.module.user.db.UserCols;
import net.bytle.tower.util.Guid;
import net.bytle.type.EmailAddress;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.db.*;

import java.time.LocalDateTime;
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

    /**
     * Add foreign key to the user table to get the email address
     */
    Map<JdbcColumn, JdbcColumn> mailingUserForeignKeys = new HashMap<>();
    mailingUserForeignKeys.put(MailingItemCols.REALM_ID, UserCols.REALM_ID);
    mailingUserForeignKeys.put(MailingItemCols.USER_ID, UserCols.ID);
    this.mailingItemTable = JdbcTable.build(jdbcSchema, "realm_mailing_item", MailingItemCols.values())
      .addPrimaryKeyColumn(MailingItemCols.REALM_ID)
      .addPrimaryKeyColumn(MailingItemCols.USER_ID)
      .addForeignKeyColumns(mailingUserForeignKeys)
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
          .create()
          .setColumn(MailingItemCols.FAILURE_COUNT, this.apiApp.getMailingFlow().getMaxCountFailureOnRow())
          .setOperator(JdbcComparisonOperator.LESS_THAN)
          .setOrNull(true)
      )
      .addPredicate(
        JdbcSingleOperatorPredicate
          .create()
          .setColumn(MailingItemCols.STATUS_CODE, MailingItemStatus.OK.getCode())
          .setOperator(JdbcComparisonOperator.NOT_EQUAL)
          .setOrNull(true)
      )
      .addEqualityPredicate(MailingItemCols.REALM_ID, mailing.getEmailRecipientList().getApp().getRealm().getGuid().getLocalId())
      .addEqualityPredicate(MailingItemCols.MAILING_ID, mailing.getLocalId())
      .addLimit(mailingJob.getItemToExecuteCount());

    return jdbcSelect.execute();
  }

  public Future<List<MailingItem>> getItemsForGraphQL(Mailing mailing, JdbcPagination pagination) {

    return JdbcPaginatedSelect.from(mailingItemTable)
      .addEqualityPredicate(MailingItemCols.REALM_ID, mailing.getRealm().getGuid().getLocalId())
      .addEqualityPredicate(MailingItemCols.MAILING_ID, mailing.getLocalId())
      .setSearchColumn(UserCols.EMAIL_ADDRESS)
      .addExtraSelectColumn(UserCols.EMAIL_ADDRESS)
      .addEqualityPredicate(UserCols.REALM_ID, mailing.getRealm().getGuid().getLocalId())
      .addOrderBy(UserCols.EMAIL_ADDRESS, JdbcSort.ASC)
      .setPagination(pagination)
      .execute(jdbcRowSet -> {
        List<MailingItem> mailingItems = new ArrayList<>();
        for (JdbcRow jdbcRow : jdbcRowSet) {
          MailingItem mailingItem = this.buildingMailingItemFromRow(jdbcRow, mailing);
          // Add address email for front end
          User user = mailingItem.getListUser().getUser();
          user.setEmailAddress(EmailAddress.ofFailSafe(jdbcRow.getString(UserCols.EMAIL_ADDRESS)));
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
    Realm realm = Realm.createFromAnyId(jdbcRow.getLong(MailingItemCols.REALM_ID));

    /**
     * Mailing
     */
    Long mailingId = jdbcRow.getLong(MailingItemCols.MAILING_ID);
    if (!Objects.equals(mailingId, mailing.getLocalId())) {
      throw new InternalException("Bad mailing id");
    }
    mailingItem.setMailing(mailing);


    /**
     * User
     */
    User user = new User();
    user.setRealm(realm);
    this.apiApp.getUserProvider().updateGuid(user, jdbcRow.getLong(MailingItemCols.USER_ID));

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
    mailingItem.setFailureCount(jdbcRow.getInteger(MailingItemCols.FAILURE_COUNT, 0));
    mailingItem.setDeliveryDate(jdbcRow.getLocalDateTime(MailingItemCols.DELIVERY_DATE));
    mailingItem.setPlannedDeliveryTime(jdbcRow.getLocalDateTime(MailingItemCols.PLANNED_DELIVERY_TIME));

    return mailingItem;
  }

  private void updateGuid(MailingItem mailingItem) {

    Guid guid = this.apiApp.createGuidStringFromRealmAndTwoObjectId(
      GUID_PREFIX,
      mailingItem.getMailing().getRealm().getGuid().getLocalId(),
      mailingItem.getMailing().getLocalId(),
      mailingItem.getListUser().getUser().getGuid().getLocalId()
    );
    mailingItem.setGuid(guid.toString());

  }

  public Future<MailingItem> getByGuidRequestHandler(String guidHash, RoutingContext routingContext, AuthUserScope authUserScope) {

    Guid guid;
    try {
      guid = this.createGuidFromHash(guidHash);
    } catch (CastException e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.BAD_STRUCTURE_422)
        .setMessage("The guid (" + guidHash + ") is not a valid mailing item guid")
        .setCauseException(e)
        .build()
      );
    }

    return this.apiApp.getRealmProvider()
      .getRealmByLocalIdWithAuthorizationCheck(guid.getRealmOrOrganizationId(), authUserScope, routingContext)
      .compose(realm -> this.apiApp.getMailingProvider().getByLocalId(guid.validateRealmAndGetFirstObjectId(realm.getGuid().getLocalId()), realm))
      .compose(mailing -> this.getItemByLocalId(guid.validateAndGetSecondObjectId(mailing.getRealm().getGuid().getLocalId()), mailing));
  }

  /**
   * @param userId the user id
   * @param mailing with a realm
   * @return The mailing item or null
   */
  private Future<MailingItem> getItemByLocalId(Long userId, Mailing mailing) {
    return JdbcSelect.from(this.mailingItemTable)
      .addEqualityPredicate(MailingItemCols.REALM_ID, mailing.getRealm().getGuid().getLocalId())
      .addEqualityPredicate(MailingItemCols.MAILING_ID, mailing.getLocalId())
      .addEqualityPredicate(MailingItemCols.USER_ID, userId)
      .addSelectColumn(UserCols.EMAIL_ADDRESS)
      .execute(jdbcRowSet -> {
        if (jdbcRowSet.size() == 0) {
          return Future.succeededFuture();
        }
        if (jdbcRowSet.size() != 1) {
          return Future.failedFuture("The mailing item selection returns more than one row");
        }
        JdbcRow jdbcRow = jdbcRowSet.iterator().next();
        MailingItem mailingItem = this.buildingMailingItemFromRow(jdbcRow, mailing);
        // Add address email for mailing
        User user = mailingItem.getListUser().getUser();
        user.setEmailAddress(EmailAddress.ofFailSafe(jdbcRow.getString(UserCols.EMAIL_ADDRESS)));
        return Future.succeededFuture(mailingItem);
      });
  }

  private Guid createGuidFromHash(String guid) throws CastException {
    return this.apiApp.createGuidFromHashWithOneRealmIdAndTwoObjectId(GUID_PREFIX, guid);
  }

  public Future<MailingItem> update(MailingItem mailingItem, MailingItemInputProps mailingItemInputProps) {

    JdbcUpdate jdbcUpdate = JdbcUpdate.into(this.mailingItemTable)
      .addPredicateColumn(MailingItemCols.REALM_ID, mailingItem.getMailing().getRealm().getGuid().getLocalId())
      .addPredicateColumn(MailingItemCols.MAILING_ID, mailingItem.getMailing().getLocalId())
      .addPredicateColumn(MailingItemCols.USER_ID, mailingItem.getListUser().getUser().getGuid().getLocalId());

    MailingItemStatus newStatus = mailingItemInputProps.getStatus();
    if (newStatus != null && newStatus.getCode() > mailingItem.getStatus().getCode()) {
      mailingItem.setStatus(newStatus);
      jdbcUpdate.addUpdatedColumn(MailingItemCols.STATUS_CODE, newStatus.getCode());
    }

    String newStatusMessage = mailingItemInputProps.getStatusMessage();
    if (newStatusMessage != null) {
      mailingItem.setStatusMessage(newStatusMessage);
      jdbcUpdate.addUpdatedColumn(MailingItemCols.STATUS_MESSAGE, newStatusMessage);
    }

    MailingJob newMailingJob = mailingItemInputProps.getMailingJob();
    if (newMailingJob != null) {
      mailingItem.setMailingJob(newMailingJob);
      jdbcUpdate.addUpdatedColumn(MailingItemCols.MAILING_JOB_ID, newMailingJob.getLocalId());
    }

    LocalDateTime plannedDeliveryTime = mailingItemInputProps.getPlannedDeliveryTime();
    if (plannedDeliveryTime != null) {
      mailingItem.setPlannedDeliveryTime(plannedDeliveryTime);
      jdbcUpdate.addUpdatedColumn(MailingItemCols.PLANNED_DELIVERY_TIME, plannedDeliveryTime);
    }

    Integer failureCount = mailingItemInputProps.getFailureCount();
    if (failureCount != null) {
      mailingItem.setFailureCount(failureCount);
      jdbcUpdate.addUpdatedColumn(MailingItemCols.FAILURE_COUNT, failureCount);
    }

    String messageId = mailingItemInputProps.getMessageId();
    if (messageId != null) {
      mailingItem.setEmailMessageId(messageId);
      jdbcUpdate.addUpdatedColumn(MailingItemCols.EMAIL_MESSAGE_ID, failureCount);
    }

    LocalDateTime deliveryDate = mailingItemInputProps.getDeliveryDate();
    if (deliveryDate != null) {
      mailingItem.setDeliveryDate(deliveryDate);
      jdbcUpdate.addUpdatedColumn(MailingItemCols.DELIVERY_DATE, deliveryDate);
    }


    if (jdbcUpdate.hasNoColumnToUpdate()) {
      return Future.succeededFuture(mailingItem);
    }

    return jdbcUpdate
      .execute()
      .compose(v -> Future.succeededFuture(mailingItem));

  }
}
