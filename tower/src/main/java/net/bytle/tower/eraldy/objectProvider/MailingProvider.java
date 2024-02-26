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
import net.bytle.vertx.TowerFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public static final String LIST_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "rcpt" + COLUMN_PART_SEP + ListProvider.LIST_ID_COLUMN;

  private static final String MAILING_ORGA_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + OrganizationProvider.ORGA_ID_COLUMN;
  public static final String MAILING_AUTHOR_USER_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "author" + COLUMN_PART_SEP + UserProvider.ID_COLUMN;
  static final String MAILING_ID_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "id";
  static final String MAILING_NAME_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "name";
  static final String SUBJECT_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "subject";
  private static final String MAILING_REALM_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + RealmProvider.REALM_ID_COLUMN;
  private static final String MAILING_STATUS_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + "status";
  static final String MAILING_GUID_PREFIX = "mai";
  private final EraldyApiApp apiApp;

  private static final String MAILING_MODIFICATION_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
  private static final String MAILING_CREATION_COLUMN = MAILING_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
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


  public Future<Mailing> insertMailing(Mailing mailing) {

    if (mailing.getLocalId() != null) {
      return Future.failedFuture(new InternalException("A mailing to insert should not have a local id"));
    }

    final String insertSql = "INSERT INTO\n" +
      FULL_QUALIFIED_TABLE_NAME + " (\n" +
      "  " + MAILING_REALM_COLUMN + ",\n" +
      "  " + MAILING_ID_COLUMN + ",\n" +
      "  " + MAILING_NAME_COLUMN + ",\n" +
      "  " + SUBJECT_COLUMN + ",\n" +
      "  " + LIST_COLUMN + ",\n" +
      "  " + MAILING_ORGA_COLUMN + ",\n" +
      "  " + MAILING_AUTHOR_USER_COLUMN + ",\n" +
      "  " + MAILING_CREATION_COLUMN + ",\n" +
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
        Mailing mailing = new Mailing();
        mailing.setLocalId(localId);
        mailing.setRealm(realm);
        // realm and id should be first set for guid update
        this.updateGuid(mailing);
        mailing.setName(row.getString(MAILING_NAME_COLUMN));
        mailing.setEmailSubject(row.getString(SUBJECT_COLUMN));
        mailing.setStatus(row.getInteger(MAILING_STATUS_COLUMN));
        mailing.setCreationTime(row.getLocalDateTime(MAILING_CREATION_COLUMN));
        mailing.setModificationTime(row.getLocalDateTime(MAILING_MODIFICATION_COLUMN));

        // file system is not yet done
        mailing.setEmailBody(null);

        // orga user
        Long orgaId = row.getLong(MAILING_ORGA_COLUMN);
        assert Objects.equals(realm.getOrganization().getLocalId(), orgaId);
        Long userId = row.getLong(MAILING_AUTHOR_USER_COLUMN);
        Future<OrganizationUser> authorFuture = this.apiApp.getOrganizationUserProvider()
          .getOrganizationUserByLocalId(userId, realm.getLocalId(), realm);

        // list
        Long listId = row.getLong(LIST_COLUMN);
        Future<ListObject> listFuture = this.apiApp.getListProvider()
          .getListById(listId, realm);

        return Future.all(authorFuture, listFuture)
          .recover(err -> Future.failedFuture(new InternalException("Future all of mailing building failed", err)))
          .compose(compositeFuture -> {
            boolean futureFailed = compositeFuture.failed(0);
            if (futureFailed) {
              Throwable err = compositeFuture.cause(0);
              return Future.failedFuture(new InternalException("Retrieving the mailing author gives us this error: " + err.getMessage(), err));
            }
            futureFailed = compositeFuture.failed(1);
            if (futureFailed) {
              Throwable err = compositeFuture.cause(1);
              return Future.failedFuture(new InternalException("Retrieving the mailing list gives us this error: " + err.getMessage(), err));
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

  public Future<List<Mailings>> getMailingsByList(ListObject list) {
    final String sql = "select * from " + FULL_QUALIFIED_TABLE_NAME + " where " + LIST_COLUMN + " = $1 and " + MAILING_REALM_COLUMN + " = $2";
    Tuple tuple = Tuple.of(list.getLocalId(), list.getRealm().getLocalId());
    return this.jdbcPool
      .preparedQuery(sql)
      .execute(tuple)
      .recover(err -> Future.failedFuture(new InternalException("Getting mailings for the list (" + tuple + ") failed. Error: " + err.getMessage() + ". Sql:\n" + sql, err)))
      .compose(rows -> {
        List<Mailings> mailingList = new ArrayList<>();
        for (Row row : rows) {
          Mailings mailings = new Mailings();
          mailings.setGuid(this.getGuidHash(row.getLong(MAILING_REALM_COLUMN), row.getLong(MAILING_ID_COLUMN)));
          mailings.setName(row.getString(MAILING_NAME_COLUMN));
          mailings.setCreationTime(row.getLocalDateTime(MAILING_CREATION_COLUMN));
          mailings.setStatus(row.getInteger(MAILING_STATUS_COLUMN));
          mailingList.add(mailings);
        }
        return Future.succeededFuture(mailingList);
      });

  }

  private String getGuidHash(Long realmId, Long mailingId) {
    return apiApp.createGuidFromRealmAndObjectId(MAILING_GUID_PREFIX, realmId, mailingId).toString();
  }

  /**
   * Update the name and author
   * @param mailing - the mailing
   * @return the same mailing
   */
  public Future<Mailing> updateMailingNameAndAuthor(Mailing mailing) {
    final String sql = "update " + FULL_QUALIFIED_TABLE_NAME + " set \n"
      + MAILING_NAME_COLUMN + " = $1,\n"
      + MAILING_AUTHOR_USER_COLUMN + " = $2,\n"
      + MAILING_ORGA_COLUMN + " = $3,\n"
      + MAILING_MODIFICATION_COLUMN + " = $4\n"
      + "where\n"
      + MAILING_ID_COLUMN + " = $5\n" +
      " and " + MAILING_REALM_COLUMN + " = $6\n"
      + "RETURNING " + MAILING_ID_COLUMN; // to check if the update has touched a row
    Tuple tuple = Tuple.of(
      mailing.getName(),
      mailing.getEmailAuthor().getLocalId(),
      mailing.getEmailAuthor().getOrganization().getLocalId(),
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
  }
}
