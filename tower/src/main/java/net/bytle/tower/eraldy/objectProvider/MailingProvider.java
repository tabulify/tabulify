package net.bytle.tower.eraldy.objectProvider;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.flow.mailing.MailingStatus;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.graphql.pojo.input.MailingInputProps;
import net.bytle.tower.eraldy.mixin.*;
import net.bytle.tower.eraldy.model.manual.Mailing;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.*;
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

  protected static final String TABLE_NAME = "realm_mailing";
  private static final String FULL_QUALIFIED_TABLE_NAME = JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME;

  public static final String COLUMN_PART_SEP = JdbcSchemaManager.COLUMN_PART_SEP;
  private static final String MAILING_PREFIX = "mailing";
  public static final String EMAIL_RCPT_LIST_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "email_rcpt" + COLUMN_PART_SEP + ListProvider.LIST_ID_COLUMN;

  private static final String MAILING_ORGA_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + OrganizationProvider.ORGA_ID_COLUMN;
  public static final String MAILING_EMAIL_AUTHOR_USER_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "email_author" + COLUMN_PART_SEP + UserProvider.ID_COLUMN;
  static final String MAILING_ID_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "id";
  static final String MAILING_NAME_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "name";
  private static final String MAILING_REALM_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + RealmProvider.REALM_ID_COLUMN;
  private static final String MAILING_STATUS_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "status";
  private static final String MAILING_EMAIL_SUBJECT = MAILING_PREFIX + COLUMN_PART_SEP + "email_subject";
  private static final String MAILING_EMAIL_PREVIEW = MAILING_PREFIX + COLUMN_PART_SEP + "email_preview";
  private static final String MAILING_EMAIL_BODY = MAILING_PREFIX + COLUMN_PART_SEP + "email_body";

  private static final String MAILING_EMAIL_LANGUAGE = MAILING_PREFIX + COLUMN_PART_SEP + "email_language";
  static final String MAILING_GUID_PREFIX = "mai";
  private final EraldyApiApp apiApp;

  private static final String MAILING_MODIFICATION_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
  private static final String MAILING_CREATION_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  private static final String MAILING_JOB_LAST_EXECUTION_TIME = MAILING_PREFIX + COLUMN_PART_SEP + "job_last_execution_time";
  private static final String MAILING_JOB_NEXT_EXECUTION_TIME = MAILING_PREFIX + COLUMN_PART_SEP + "job_next_execution_time";
  private static final String MAILING_COUNT_ROW = MAILING_PREFIX + COLUMN_PART_SEP + "count_row";
  private static final String MAILING_COUNT_ROW_SUCCESS = MAILING_PREFIX + COLUMN_PART_SEP + "count_row_success";
  private static final String MAILING_COUNT_ROW_EXECUTION = MAILING_PREFIX + COLUMN_PART_SEP + "count_row_execution";
  private final Pool jdbcPool;
  private final JsonMapper apiMapper;


  public MailingProvider(EraldyApiApp apiApp) {
    this.apiApp = apiApp;
    Server server = apiApp.getHttpServer().getServer();
    this.jdbcPool = server.getPostgresClient().getPool();
    this.apiMapper = server.getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(Mailing.class, MailingPublicMixin.class)
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(App.class, AppPublicMixinWithoutRealm.class)
      .addMixIn(ListObject.class, ListItemMixinWithoutRealm.class)
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
        final String insertSql = "INSERT INTO\n" +
          FULL_QUALIFIED_TABLE_NAME + " (\n" +
          "  " + MAILING_REALM_COLUMN + ",\n" +
          "  " + MAILING_ID_COLUMN + ",\n" +
          "  " + MAILING_NAME_COLUMN + ",\n" +
          "  " + EMAIL_RCPT_LIST_COLUMN + ",\n" +
          "  " + MAILING_ORGA_COLUMN + ",\n" +
          "  " + MAILING_EMAIL_AUTHOR_USER_COLUMN + ",\n" +
          "  " + MAILING_CREATION_COLUMN + ",\n" +
          "  " + MAILING_STATUS_COLUMN + "\n" +
          "  )\n" +
          " values ($1, $2, $3, $4, $5, $6, $7, $8)";

        Mailing mailing = new Mailing();
        return jdbcPool
          .withTransaction(sqlConnection ->
            this.apiApp.getRealmSequenceProvider()
              .getNextIdForTableAndRealm(sqlConnection, list.getRealm(), TABLE_NAME)
              .compose(nextId -> {

                mailing.setLocalId(nextId);
                mailing.setRealm(list.getRealm());
                updateGuid(mailing);
                String name = mailingInputProps.getName();
                if (name == null) {
                  name = "Mailing of " + LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE);
                }
                mailing.setName(name);
                mailing.setEmailRecipientList(list);
                OrganizationUser ownerUser = ListProvider.getOwnerUser(list);
                mailing.setEmailAuthor(ownerUser);

                return sqlConnection
                  .preparedQuery(insertSql)
                  .execute(Tuple.of(
                    mailing.getRealm().getLocalId(),
                    mailing.getLocalId(),
                    mailing.getName(),
                    mailing.getEmailRecipientList().getLocalId(),
                    mailing.getEmailAuthor().getOrganization().getLocalId(),
                    mailing.getEmailAuthor().getLocalId(),
                    DateTimeUtil.getNowInUtc(),
                    MailingStatus.OPEN.getCode()
                  ));
              })
              .recover(e -> Future.failedFuture(new InternalException("Mailing creation Error: Sql Error " + e.getMessage(), e)))
              .compose(rows -> Future.succeededFuture(mailing)));
      });


  }


  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }

  public Future<Mailing> getByLocalId(Long localId, Realm realm) {

    final String selectSql = "select * from " + FULL_QUALIFIED_TABLE_NAME + " where " + MAILING_REALM_COLUMN + " = $1 and " + MAILING_ID_COLUMN + " =$2";
    Tuple tuple = Tuple.of(
      realm.getLocalId(),
      localId
    );
    return this.jdbcPool
      .preparedQuery(selectSql)
      .execute(tuple)
      .recover(err -> Future.failedFuture(
          new InternalException("The select of the mailing (" + tuple + ") returns the following error: " + err.getMessage() + "\n" + selectSql, err)
        )
      )
      .compose(rows -> {
        if (rows.size() == 0) {
          return Future.succeededFuture();
        }
        if (rows.size() != 1) {
          return Future.failedFuture(
            new InternalException("The select of the mailing (" + tuple + ") returns more than one rows (" + rows.size() + ")")
          );
        }

        Row row = rows.iterator().next();
        Mailing mailing = this.buildFromRow(row, realm);

        return Future.succeededFuture(mailing);

      });
  }

  private Mailing buildFromRow(Row row, Realm realm) {
    Mailing mailing = new Mailing();
    mailing.setLocalId(row.getLong(MAILING_ID_COLUMN));
    mailing.setRealm(realm);
    // realm and id should be first set for guid update
    this.updateGuid(mailing);
    mailing.setName(row.getString(MAILING_NAME_COLUMN));
    mailing.setCreationTime(row.getLocalDateTime(MAILING_CREATION_COLUMN));
    mailing.setModificationTime(row.getLocalDateTime(MAILING_MODIFICATION_COLUMN));

    /**
     * Email
     */
    mailing.setEmailSubject(row.getString(MAILING_EMAIL_SUBJECT));
    mailing.setEmailPreview(row.getString(MAILING_EMAIL_PREVIEW));
    mailing.setEmailBody(row.getString(MAILING_EMAIL_BODY));
    mailing.setEmailLanguage(row.getString(MAILING_EMAIL_LANGUAGE));

    /**
     * Orga User
     * The full user is retrieved if requested
     * (In graphQl, by the function that is mapped to the type)
     * ie {@link #getEmailAuthorAtRequestTime(Mailing)}
     */
    Long orgaId = row.getLong(MAILING_ORGA_COLUMN);
    assert Objects.equals(realm.getOrganization().getLocalId(), orgaId);
    Long userId = row.getLong(MAILING_EMAIL_AUTHOR_USER_COLUMN);
    OrganizationUser authorUser = new OrganizationUser();
    authorUser.setLocalId(userId);
    authorUser.setRealm(realm);
    mailing.setEmailAuthor(authorUser);

    /**
     * List
     * The full list object is retrieved by the API
     * (In graphQl, by the function that is mapped to the type)
     * {@link net.bytle.tower.eraldy.graphql.implementer.MailingGraphQLImpl#getMailingRecipientList(DataFetchingEnvironment)}
     */
    Long listId = row.getLong(EMAIL_RCPT_LIST_COLUMN);
    ListObject recipientList = new ListObject();
    recipientList.setLocalId(listId);
    recipientList.setRealm(realm);
    mailing.setEmailRecipientList(recipientList);

    /**
     * Job
     */
    Integer statusCode = row.getInteger(MAILING_STATUS_COLUMN);
    MailingStatus status;
    if (statusCode != null) {
      status = MailingStatus.fromStatusCodeFailSafe(statusCode);
    } else {
      status = MailingStatus.OPEN;
    }
    mailing.setStatus(status);
    mailing.setJobLastExecutionTime(row.getLocalDateTime(MAILING_JOB_LAST_EXECUTION_TIME));
    mailing.setJobNextExecutionTime(row.getLocalDateTime(MAILING_JOB_NEXT_EXECUTION_TIME));
    mailing.setCountRow(row.getInteger(MAILING_COUNT_ROW));
    mailing.setCountRowSuccess(row.getInteger(MAILING_COUNT_ROW_SUCCESS));
    mailing.setCountRowExecution(row.getInteger(MAILING_COUNT_ROW_EXECUTION));

    return mailing;

  }

  public Guid getGuid(String mailingIdentifier) throws CastException {
    return this.apiApp.createGuidFromHashWithOneRealmIdAndOneObjectId(MAILING_GUID_PREFIX, mailingIdentifier);
  }

  public Future<List<Mailing>> getMailingsByListWithLocalId(long listId, Realm realm) {
    final String sql = "select * from " + FULL_QUALIFIED_TABLE_NAME + " where " + EMAIL_RCPT_LIST_COLUMN + " = $1 and " + MAILING_REALM_COLUMN + " = $2";
    Tuple tuple = Tuple.of(listId, realm.getLocalId());
    return this.jdbcPool
      .preparedQuery(sql)
      .execute(tuple)
      .recover(err -> Future.failedFuture(new InternalException("Getting mailings for the list (" + tuple + ") failed. Error: " + err.getMessage() + ". Sql:\n" + sql, err)))
      .compose(rows -> {
        List<Mailing> mailingList = new ArrayList<>();
        for (Row row : rows) {
          Mailing mailing = this.buildFromRow(row, realm);
          mailingList.add(mailing);
        }
        return Future.succeededFuture(mailingList);
      });

  }

  private String getGuidHash(Long realmId, Long mailingId) {
    return apiApp.createGuidFromRealmAndObjectId(MAILING_GUID_PREFIX, realmId, mailingId).toString();
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

        /**
         * Patch implementation
         */
        String newName = mailingInputProps.getName();
        if (newName != null) {
          mailing.setName(newName);
        }

        String subject = mailingInputProps.getEmailSubject();
        if (subject != null) {
          mailing.setEmailSubject(subject);
        }

        String emailLanguage = mailingInputProps.getEmailLanguage();
        if (emailLanguage != null) {
          mailing.setEmailLanguage(emailLanguage);
        }

        String preview = mailingInputProps.getEmailPreview();
        if (preview != null) {
          mailing.setEmailPreview(preview);
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
        }

        LocalDateTime jobNextExecutionTime = mailingInputProps.getJobNextExecutionTime();
        if(jobNextExecutionTime!=null){
          mailing.setJobNextExecutionTime(jobNextExecutionTime);
        }

        String newAuthorGuid = mailingInputProps.getEmailAuthorGuid();
        Future<OrganizationUser> newAuthorFuture;
        if (newAuthorGuid != null) {
          newAuthorFuture = this.apiApp.getOrganizationUserProvider().getOrganizationUserByIdentifier(newAuthorGuid);
        } else {
          newAuthorFuture = this.getEmailAuthorAtRequestTime(mailing);
        }
        return newAuthorFuture.compose(newAuthor -> {

          mailing.setEmailAuthor(newAuthor);
          final String sql = "update " + FULL_QUALIFIED_TABLE_NAME + " set \n"
            + MAILING_NAME_COLUMN + " = $1,\n"
            + MAILING_EMAIL_AUTHOR_USER_COLUMN + " = $2,\n"
            + MAILING_ORGA_COLUMN + " = $3,\n"
            + MAILING_EMAIL_SUBJECT + " = $4,\n"
            + MAILING_EMAIL_PREVIEW + " = $5,\n"
            + MAILING_EMAIL_BODY + " = $6,\n"
            + MAILING_EMAIL_LANGUAGE + " = $7,\n"
            + MAILING_JOB_NEXT_EXECUTION_TIME + " = $8,\n"
            + MAILING_MODIFICATION_COLUMN + " = $9\n"
            + "where\n"
            + MAILING_ID_COLUMN + " = $10\n" +
            " and " + MAILING_REALM_COLUMN + " = $11\n"
            + "RETURNING " + MAILING_ID_COLUMN; // to check if the update has touched a row
          Tuple tuple = Tuple.of(
            mailing.getName(),
            mailing.getEmailAuthor().getLocalId(),
            mailing.getEmailAuthor().getOrganization().getLocalId(),
            mailing.getEmailSubject(),
            mailing.getEmailPreview(),
            mailing.getEmailBody(),
            mailing.getEmailLanguage(),
            mailing.getJobNextExecutionTime(),
            DateTimeUtil.getNowInUtc(),
            mailing.getLocalId(),
            mailing.getRealm().getLocalId()
          );

          return this.jdbcPool
            .preparedQuery(sql)
            .execute(tuple)
            .recover(err -> Future.failedFuture(new InternalException("Updating mailing (" + tuple.deepToString() + ") failed. Error: " + err.getMessage() + ". Sql:\n" + sql, err)))
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
      });

  }

  /**
   * A request handler that returns a mailing by guid or null if not found
   * (used in the rest and graphql api)
   */
  public Future<Mailing> getByGuidRequestHandler(String mailingGuidIdentifier, RoutingContext routingContext, AuthUserScope scope) {


    Guid guid;
    try {
      guid = this.getGuid(mailingGuidIdentifier);
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

}
