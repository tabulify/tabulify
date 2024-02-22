package net.bytle.tower.eraldy.objectProvider;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.mixin.*;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.DateTimeUtil;
import net.bytle.vertx.JdbcSchemaManager;
import net.bytle.vertx.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public static final String LIST_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "rcpt" + COLUMN_PART_SEP + ListProvider.ID_COLUMN;

  private static final String ORGA_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + OrganizationProvider.ORGA_ID_COLUMN;
  public static final String AUTHOR_USER_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "author" + COLUMN_PART_SEP + UserProvider.ID_COLUMN;
  static final String ID_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "id";
  static final String NAME_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "name";
  static final String SUBJECT_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "subject";
  private static final String REALM_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + RealmProvider.ID_COLUMN;
  private static final String MAILING_STATUS_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "status";
  static final String MAILING_GUID_PREFIX = "mai";
  private final EraldyApiApp apiApp;

  private static final String MODIFICATION_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
  private static final String CREATION_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
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
    String guid = apiApp.createGuidFromRealmAndObjectId(MAILING_GUID_PREFIX, mailing.getRealm(), mailing.getLocalId()).toString();
    mailing.setGuid(guid);
  }


  public Future<Mailing> insertMailing(Mailing mailing) {

    if (mailing.getLocalId() != null) {
      return Future.failedFuture(new InternalException("A mailing to insert should not have a local id"));
    }

    final String insertSql = "INSERT INTO\n" +
      FULL_QUALIFIED_TABLE_NAME + " (\n" +
      "  " + REALM_COLUMN + ",\n" +
      "  " + ID_COLUMN + ",\n" +
      "  " + NAME_COLUMN + ",\n" +
      "  " + SUBJECT_COLUMN + ",\n" +
      "  " + LIST_COLUMN + ",\n" +
      "  " + ORGA_COLUMN + ",\n" +
      "  " + AUTHOR_USER_COLUMN + ",\n" +
      "  " + CREATION_COLUMN + ",\n" +
      "  " + MAILING_STATUS_COLUMN + "\n" +
      "  )\n" +
      " values ($1, $2, $3, $4, $5, $6, $7, $8, $9)";


    return jdbcPool
      .withTransaction(sqlConnection ->
        this.apiApp.getRealmSequenceProvider()
          .getNextIdForTableAndRealm(sqlConnection, mailing.getRealm(), TABLE_NAME)
          .compose(nextId -> {
            mailing.setLocalId(nextId);
            updateGuid(mailing);
            return sqlConnection
              .preparedQuery(insertSql)
              .execute(Tuple.of(
                mailing.getRealm().getLocalId(),
                mailing.getLocalId(),
                mailing.getName(),
                mailing.getEmailSubject(),
                mailing.getRecipientList().getLocalId(),
                mailing.getEmailAuthor().getOrganization().getLocalId(),
                mailing.getEmailAuthor().getLocalId(),
                DateTimeUtil.getNowInUtc(),
                0
              ));
          })
          .recover(e -> Future.failedFuture(new InternalException("Mailing creation Error: Sql Error " + e.getMessage(), e)))
          .compose(rows -> Future.succeededFuture(mailing)));
  }


  public ObjectMapper getApiMapper() {
    return this.apiMapper;
  }

  public Future<Mailing> getByLocalId(Long localId, Realm realm) {

    final String selectSql = "select * from " + FULL_QUALIFIED_TABLE_NAME + " where " + REALM_COLUMN + " = $1 and " + ID_COLUMN + " =$2";
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
        Mailing mailing = new Mailing();
        mailing.setRealm(realm);
        mailing.setName(row.getString(NAME_COLUMN));
        mailing.setEmailSubject(row.getString(SUBJECT_COLUMN));
        mailing.setStatus(row.getInteger(MAILING_STATUS_COLUMN));
        mailing.setCreationTime(row.getLocalDateTime(CREATION_COLUMN));
        mailing.setModificationTime(row.getLocalDateTime(MODIFICATION_COLUMN));

        // file system is not yet done
        mailing.setEmailBody(null);

        // orga user
        Long orgaId = row.getLong(ORGA_COLUMN);
        assert Objects.equals(realm.getOrganization().getLocalId(), orgaId);
        Long userId = row.getLong(AUTHOR_USER_COLUMN);
        Future<OrganizationUser> authorFuture = this.apiApp.getOrganizationUserProvider()
          .getOrganizationUserByLocalId(userId, realm.getLocalId(), realm);

        // list
        Long listId = row.getLong(LIST_COLUMN);
        Future<ListObjectAnalytics> listFuture = this.apiApp.getListProvider()
          .getListById(listId, realm, ListObjectAnalytics.class);

        return Future.all(authorFuture, listFuture)
          .recover(err->Future.failedFuture(new InternalException("Future all of mailing building failed",err)))
          .compose(compositeFuture -> {
            boolean futureFailed = compositeFuture.failed(0);
            if(futureFailed){
              Throwable err = compositeFuture.cause(0);
              return Future.failedFuture(new InternalException("Retrieving the mailing author gives us this error: "+err.getMessage(),err));
            }
            futureFailed = compositeFuture.failed(1);
            if(futureFailed){
              Throwable err = compositeFuture.cause(1);
              return Future.failedFuture(new InternalException("Retrieving the mailing list gives us this error: "+err.getMessage(),err));
            }
            OrganizationUser authorUser = compositeFuture.resultAt(0);
            ListObject recipientList = compositeFuture.resultAt(1);
            mailing.setEmailAuthor(authorUser);
            mailing.setRecipientList(recipientList);
            return Future.succeededFuture(mailing);
          });

      });
  }

  public Guid getGuid(String mailingIdentifier) throws CastException {
    return this.apiApp.createGuidFromHashWithOneRealmIdAndOneObjectId(MAILING_GUID_PREFIX, mailingIdentifier);
  }
}
