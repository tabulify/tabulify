package net.bytle.tower.eraldy.module.mailing.db.mailing;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.mixin.*;
import net.bytle.tower.eraldy.model.manual.Status;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.module.list.db.ListProvider;
import net.bytle.tower.eraldy.module.mailing.graphql.MailingGraphQLImpl;
import net.bytle.tower.eraldy.module.mailing.inputs.MailingInputProps;
import net.bytle.tower.eraldy.module.mailing.jackson.JacksonMailingStatusDeserializer;
import net.bytle.tower.eraldy.module.mailing.model.Mailing;
import net.bytle.tower.eraldy.module.mailing.model.MailingStatus;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.Server;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.db.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manage the get/upsert of a {@link Mailing} object asynchronously
 */
public class MailingProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(MailingProvider.class);

  static final String MAILING_PREFIX = "mailing";


  static final String MAILING_GUID_PREFIX = "mai";
  private final EraldyApiApp apiApp;


  private final Pool jdbcPool;
  private final JsonMapper apiMapper;
  private final String updateItemCountAndStatusToRunningSqlStatement;

  /**
   * Sql to insert the mailing job rows
   * from a mailing job
   */
  private final String mailingItemsSqlInsertion;
  private final JdbcTable mailingTable;

  public MailingProvider(EraldyApiApp apiApp, JdbcSchema jdbcSchema) {
    this.apiApp = apiApp;
    Server server = apiApp.getHttpServer().getServer();
    JdbcClient postgresClient = server.getPostgresClient();
    this.jdbcPool = postgresClient.getPool();
    this.apiMapper = server.getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(Mailing.class, MailingPublicMixin.class)
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(App.class, AppPublicMixinWithoutRealm.class)
      .addMixIn(ListObject.class, ListItemMixinWithoutRealm.class)
      .build();

    this.updateItemCountAndStatusToRunningSqlStatement = postgresClient.getSqlStatement("mailing/mailing-update-item-count-and-state.sql");
    this.mailingItemsSqlInsertion = postgresClient.getSqlStatement("mailing/mailing-item-insertion.sql");


    this.apiApp.getHttpServer().getServer().getJacksonMapperManager()
      .addDeserializer(MailingStatus.class, new JacksonMailingStatusDeserializer());

    this.mailingTable = JdbcTable.build(jdbcSchema, "realm_mailing")
      .build();

  }


  /**
   * This function was created to be sure that the data is consistent
   * between guid and (id and realm id)
   *
   * @param mailing - the mailing
   */
  private void updateGuid(Mailing mailing) {
    if (mailing.getGuid() != null) {
      return;
    }
    String guid = this.getGuidHash(mailing.getRealm().getLocalId(), mailing.getLocalId());
    mailing.setGuid(guid);
  }


  public Future<Mailing> insertMailingRequestHandler(String listGuid, MailingInputProps mailingInputProps, RoutingContext routingContext) {

    Guid listGuidObject;
    try {
      listGuidObject = this.apiApp.getListProvider().getGuidObject(listGuid);
    } catch (CastException e) {
      return Future.failedFuture(new IllegalArgumentException("The list guid (" + listGuid + ") is not valid", e));
    }

    return this.apiApp.getRealmProvider()
      .getRealmByLocalIdWithAuthorizationCheck(listGuidObject.getRealmOrOrganizationId(), AuthUserScope.MAILING_CREATE, routingContext)
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setMessage("The realm of the list (" + listGuid + ") was not found")
            .setType(TowerFailureTypeEnum.NOT_FOUND_404)
            .build()
          );
        }
        return this.apiApp.getListProvider()
          .getListById(listGuidObject.validateRealmAndGetFirstObjectId(realm.getLocalId()), realm);
      })
      .compose(list -> {

        if (list == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setMessage("The list (" + listGuid + ") was not found")
            .setType(TowerFailureTypeEnum.NOT_FOUND_404)
            .build()
          );
        }

        Mailing mailing = new Mailing();
        JdbcInsert jdbcInsert = JdbcInsert.into(this.mailingTable);

        // status
        mailing.setStatus(MailingStatus.OPEN);
        jdbcInsert.addColumn(MailingCols.STATUS_CODE, mailing.getStatus().getCode());

        // creation time
        mailing.setCreationTime(DateTimeService.getNowInUtc());
        jdbcInsert.addColumn(MailingCols.CREATION_TIME, mailing.getCreationTime());

        // realm
        mailing.setRealm(list.getRealm());
        jdbcInsert.addColumn(MailingCols.REALM_ID, mailing.getRealm().getLocalId());

        // name
        String name = mailingInputProps.getName();
        if (name == null) {
          name = "Mailing of " + LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE);
        }
        mailing.setName(name);
        jdbcInsert.addColumn(MailingCols.NAME, mailing.getName());

        // list
        mailing.setEmailRecipientList(list);
        jdbcInsert.addColumn(MailingCols.EMAIL_RCPT_LIST_ID, mailing.getEmailRecipientList().getLocalId());

        // owner
        OrganizationUser ownerUser = ListProvider.getOwnerUser(list);
        mailing.setEmailAuthor(ownerUser);
        jdbcInsert.addColumn(MailingCols.EMAIL_AUTHOR_USER_ID, ownerUser.getLocalId());
        jdbcInsert.addColumn(MailingCols.ORGA_ID, ownerUser.getOrganization().getLocalId());

        return jdbcPool
          .withTransaction(sqlConnection ->
            this.apiApp.getRealmSequenceProvider()
              .getNextIdForTableAndRealm(sqlConnection, list.getRealm(), this.mailingTable)
              .compose(nextId -> {

                // local id
                mailing.setLocalId(nextId);
                jdbcInsert.addColumn(MailingCols.ID, nextId);
                updateGuid(mailing);

                return jdbcInsert.execute();
              })
              .compose(rows -> Future.succeededFuture(mailing)));
      });


  }


  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }

  public Future<Mailing> getByLocalId(Long localId, Realm realm) {


    return JdbcSelect
      .from(this.mailingTable)
      .addEqualityPredicate(MailingCols.REALM_ID, realm.getLocalId())
      .addEqualityPredicate(MailingCols.ID, localId)
      .execute()
      .compose(rows -> {
        if (rows.size() == 0) {
          return Future.succeededFuture();
        }
        if (rows.size() != 1) {
          return Future.failedFuture(
            new InternalException("The select of the mailing returns more than one rows (" + rows.size() + ")")
          );
        }

        JdbcRow row = rows.iterator().next();
        Mailing mailing = this.buildFromRow(row, realm);

        return Future.succeededFuture(mailing);

      });
  }

  private Mailing buildFromRow(JdbcRow row, Realm realm) {
    Mailing mailing = new Mailing();
    mailing.setLocalId(row.getLong(MailingCols.ID));
    mailing.setRealm(realm);
    // realm and id should be first set for guid update
    this.updateGuid(mailing);
    mailing.setName(row.getString(MailingCols.NAME));
    mailing.setCreationTime(row.getLocalDateTime(MailingCols.CREATION_TIME));
    mailing.setModificationTime(row.getLocalDateTime(MailingCols.MODIFICATION_TIME));

    /**
     * Email
     */
    mailing.setEmailSubject(row.getString(MailingCols.EMAIL_SUBJECT));
    mailing.setEmailPreview(row.getString(MailingCols.EMAIL_PREVIEW));
    mailing.setEmailBody(row.getString(MailingCols.EMAIL_BODY));
    mailing.setEmailLanguage(row.getString(MailingCols.EMAIL_LANGUAGE));

    /**
     * Orga User
     * The full user is retrieved if requested
     * (In graphQl, by the function that is mapped to the type)
     * ie {@link #getEmailAuthorAtRequestTime(Mailing)}
     */
    Long orgaId = row.getLong(MailingCols.ORGA_ID);
    assert Objects.equals(realm.getOrganization().getLocalId(), orgaId);
    Long userId = row.getLong(MailingCols.EMAIL_AUTHOR_USER_ID);
    OrganizationUser authorUser = new OrganizationUser();
    authorUser.setLocalId(userId);
    authorUser.setRealm(realm);
    mailing.setEmailAuthor(authorUser);

    /**
     * List
     * The full list object is retrieved by the API
     * (In graphQl, by the function that is mapped to the type)
     * {@link MailingGraphQLImpl#getMailingRecipientList(DataFetchingEnvironment)}
     */
    Long listId = row.getLong(MailingCols.EMAIL_RCPT_LIST_ID);
    ListObject recipientList = new ListObject();
    recipientList.setLocalId(listId);
    recipientList.setRealm(realm);
    mailing.setEmailRecipientList(recipientList);

    /**
     * Job
     */
    Integer statusCode = row.getInteger(MailingCols.STATUS_CODE);
    MailingStatus status;
    if (statusCode != null) {
      status = MailingStatus.fromStatusCodeFailSafe(statusCode);
    } else {
      status = MailingStatus.OPEN;
    }
    mailing.setStatus(status);
    mailing.setJobLastExecutionTime(row.getLocalDateTime(MailingCols.LAST_EXECUTION_TIME));
    mailing.setJobNextExecutionTime(row.getLocalDateTime(MailingCols.NEXT_EXECUTION_TIME));
    mailing.setRowCount(row.getLong(MailingCols.ITEM_COUNT));
    mailing.setRowSuccessCount(row.getLong(MailingCols.ITEM_SUCCESS_COUNT));
    mailing.setRowExecutionCount(row.getLong(MailingCols.ITEM_EXECUTION_COUNT));

    return mailing;

  }

  public Guid getGuidObject(String mailingIdentifier) throws CastException {
    return this.apiApp.createGuidFromHashWithOneRealmIdAndOneObjectId(MAILING_GUID_PREFIX, mailingIdentifier);
  }

  public Future<List<Mailing>> getMailingsByListWithLocalId(long listId, Realm realm) {

    return JdbcSelect
      .from(this.mailingTable)
      .addEqualityPredicate(MailingCols.EMAIL_RCPT_LIST_ID, listId)
      .addEqualityPredicate(MailingCols.REALM_ID, realm.getLocalId())
      .execute()
      .compose(rows -> {
        List<Mailing> mailingList = new ArrayList<>();
        for (JdbcRow row : rows) {
          Mailing mailing = this.buildFromRow(row, realm);
          mailingList.add(mailing);
        }
        return Future.succeededFuture(mailingList);
      });

  }

  private String getGuidHash(Long realmId, Long mailingId) {
    return apiApp.createGuidFromRealmAndObjectId(MAILING_GUID_PREFIX, realmId, mailingId).toString();
  }

  public Future<Mailing> updateMailing(Mailing mailing, MailingInputProps mailingInputProps) {

    /**
     * Closed?
     */
    Status actualStatus = mailing.getStatus();
    if (actualStatus == MailingStatus.COMPLETED) {
      return Future.failedFuture(TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.BAD_STATE_400)
        .setMessage("The mailing (" + mailing + ") is closed, no modifications can be performed anymore")
        .build()
      );
    }

    JdbcUpdate jdbcUpdate = JdbcUpdate.into(this.mailingTable)
      .addPredicateColumn(MailingCols.ID,mailing.getLocalId())
      .addPredicateColumn(MailingCols.REALM_ID, mailing.getRealm().getLocalId());

    /**
     * Patch implementation
     */
    String newName = mailingInputProps.getName();
    if (newName != null) {
      mailing.setName(newName);
      jdbcUpdate.addUpdatedColumn(MailingCols.NAME,mailing.getName());
    }

    String subject = mailingInputProps.getEmailSubject();
    if (subject != null) {
      mailing.setEmailSubject(subject);
      jdbcUpdate.addUpdatedColumn(MailingCols.EMAIL_SUBJECT,mailing.getEmailSubject());
    }

    String emailLanguage = mailingInputProps.getEmailLanguage();
    if (emailLanguage != null) {
      mailing.setEmailLanguage(emailLanguage);
      jdbcUpdate.addUpdatedColumn(MailingCols.EMAIL_LANGUAGE,mailing.getEmailLanguage());
    }

    String preview = mailingInputProps.getEmailPreview();
    if (preview != null) {
      mailing.setEmailPreview(preview);
      jdbcUpdate.addUpdatedColumn(MailingCols.EMAIL_PREVIEW,mailing.getEmailPreview());
    }

    String body = mailingInputProps.getEmailBody();
    if (body != null) {
      // It should be a json array because we take Rich Slate AST for now
      // later it may be HTML, so we check late
      JsonArray jsonArray;
      try {
        jsonArray = new JsonArray(body);
      } catch (Exception e) {
        return Future.failedFuture(TowerFailureException.builder()
          .setMessage("The body is not a valid json")
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .build()
        );
      }
      mailing.setEmailBody(jsonArray.toString());
      jdbcUpdate.addUpdatedColumn(MailingCols.EMAIL_BODY,mailing.getEmailBody());
    }

    LocalDateTime jobNextExecutionTime = mailingInputProps.getJobNextExecutionTime();
    if (jobNextExecutionTime != null) {
      mailing.setJobNextExecutionTime(jobNextExecutionTime);
      jdbcUpdate.addUpdatedColumn(MailingCols.NEXT_EXECUTION_TIME,mailing.getJobNextExecutionTime());
    }

    // Status at the end (it may be changed by the setting of a schedule time
    MailingStatus newStatus = mailingInputProps.getStatus();
    if (newStatus != null) {

      /**
       * Validation
       */
      if (newStatus == MailingStatus.OPEN & actualStatus != MailingStatus.OPEN) {
        return Future.failedFuture(TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("You can't set back the status to open when the status is " + actualStatus)
          .build()
        );
      }
      mailing.setStatus(newStatus);
      jdbcUpdate.addUpdatedColumn(MailingCols.STATUS_CODE,mailing.getStatus().getCode());

    }

    String newAuthorGuid = mailingInputProps.getEmailAuthorGuid();
    Future<OrganizationUser> newAuthorFuture = Future.succeededFuture();
    if (newAuthorGuid != null) {
      newAuthorFuture = this.apiApp.getOrganizationUserProvider().getOrganizationUserByIdentifier(newAuthorGuid);
    }
    return newAuthorFuture.compose(newAuthor -> {

      if(newAuthor!=null){
        mailing.setEmailAuthor(newAuthor);
        jdbcUpdate.addUpdatedColumn(MailingCols.EMAIL_AUTHOR_USER_ID,mailing.getEmailAuthor().getLocalId());
        jdbcUpdate.addUpdatedColumn(MailingCols.ORGA_ID,mailing.getEmailAuthor().getOrganization().getLocalId());
      }


      if(jdbcUpdate.hasNoColumnToUpdate()){
        return Future.succeededFuture(mailing);
      }

      // to check if the update has touched a row
      jdbcUpdate.addReturningColumn(MailingCols.ID);


      return jdbcUpdate
        .execute()
        .compose(rowSet -> {
          if (rowSet.size() != 1) {
            // 1 because we use the RETURNING SQL clause
            // 0 should not happen as we select it beforehand to build the mailing
            return Future.failedFuture(TowerFailureException.builder()
              .setMessage("Update Mailing: No mailing was updated for the tuple (" + Tuple.of(mailing.getLocalId(),
                mailing.getRealm().getLocalId()).deepToString() + ")")
              .build());
          }
          return Future.succeededFuture(mailing);
        });
    });
  }

  /**
   * Update
   *
   * @return the same mailing
   */
  public Future<Mailing> updateMailingRequestHandler(String mailingGuidIdentifier, MailingInputProps mailingInputProps, RoutingContext routingContext) {


    return this.getByGuidRequestHandler(mailingGuidIdentifier, routingContext, AuthUserScope.MAILING_UPDATE)
      .compose(mailing -> {

        if (mailing == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setMessage("The mailing (" + mailingGuidIdentifier + ") was not found")
            .setType(TowerFailureTypeEnum.NOT_FOUND_404)
            .build()
          );
        }

        return this.updateMailing(mailing, mailingInputProps);
      });

  }

  /**
   * A request handler that returns a mailing by guid or null if not found
   * (used in the rest and graphql api)
   */
  public Future<Mailing> getByGuidRequestHandler(String mailingGuidIdentifier, RoutingContext routingContext, AuthUserScope scope) {


    Guid guid;
    try {
      guid = this.getGuidObject(mailingGuidIdentifier);
    } catch (CastException e) {
      return Future.failedFuture(new IllegalArgumentException("The mailing guid (" + mailingGuidIdentifier + ") is not valid", e));
    }

    return this.apiApp.getRealmProvider()
      .getRealmByLocalIdWithAuthorizationCheck(guid.getRealmOrOrganizationId(), scope, routingContext)
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setMessage("The realm was not found")
            .setType(TowerFailureTypeEnum.NOT_FOUND_404)
            .build()
          );
        }
        long localId = guid.validateRealmAndGetFirstObjectId(realm.getLocalId());
        return this.getByLocalId(localId, realm);
      });
  }


  /**
   * Email author
   * The object is build at request time
   * (Feature of GraphQL where a type can be matched to a function)
   */
  public Future<OrganizationUser> getEmailAuthorAtRequestTime(Mailing mailing) {
    OrganizationUser emailAuthor = mailing.getEmailAuthor();
    String guid = emailAuthor.getGuid();
    if (guid != null) {
      return Future.succeededFuture(emailAuthor);
    }
    return this.apiApp.getOrganizationUserProvider()
      .getOrganizationUserByLocalId(emailAuthor.getLocalId(), emailAuthor.getRealm().getLocalId(), emailAuthor.getRealm());
  }

  public Future<ListObject> getListAtRequestTime(Mailing mailing) {
    ListObject listObject = mailing.getEmailRecipientList();
    String guid = listObject.getGuid();
    if (guid != null) {
      return Future.succeededFuture(listObject);
    }
    return this.apiApp.getListProvider()
      .getListById(listObject.getLocalId(), listObject.getRealm());
  }


  /**
   * Create the request: create the lines and change the status of the mailing
   */
  public Future<Void> createRequest(Mailing mailing) {

    /**
     * Transaction because it happens in 2 steps
     */
    return this.jdbcPool
      .withTransaction(connection -> {
        /**
         * Create the rows
         */
        return connection
          .preparedQuery(this.mailingItemsSqlInsertion)
          .execute(Tuple.of(
            ListUserStatus.OK.getValue(),
            mailing.getEmailRecipientList().getRealm().getLocalId(),
            mailing.getLocalId()
          ))
          .recover(e -> Future.failedFuture(new InternalException("Mailing Job Rows insertion err error: Sql Error " + e.getMessage(), e)))
          .compose(v -> {
            /**
             * Update mailing with the rows and state
             */
            MailingStatus processingState = MailingStatus.PROCESSING;
            return connection
              .preparedQuery(this.updateItemCountAndStatusToRunningSqlStatement)
              .execute(Tuple.of(
                processingState.getCode(),
                mailing.getRealm().getLocalId(),
                mailing.getLocalId()
              ))
              .recover(err -> Future.failedFuture(TowerFailureException.builder()
                .setMessage("Error on mailing update row count on mailing ( " + mailing + "). Error: " + err.getMessage())
                .setCauseException(err)
                .build()
              ))
              .compose(
                rowSet -> {
                  Long countRow = rowSet.iterator().next().getLong(MailingCols.ITEM_COUNT.getColumnName());
                  mailing.setRowCount(countRow);
                  mailing.setStatus(processingState);
                  return Future.succeededFuture();
                }
              );
          });
      });

  }
}
