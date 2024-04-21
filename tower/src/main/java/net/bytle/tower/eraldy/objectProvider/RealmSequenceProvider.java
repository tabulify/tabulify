package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.db.JdbcSchemaManager;
import net.bytle.vertx.db.JdbcTable;

/**
 * This class creates and maintain a sequence by realm.
 * <p>
 * We may split on storage level by realm
 * Therefore all sequence are realm based.
 * <p>
 * For instance, a list is a realm and a list sequence
 * See the data_model.md file for more information
 */
public class RealmSequenceProvider {


  private static final String TABLE_NAME = "realm_sequence";

  private static final String TABLE_PREFIX = "sequence";
  private static final String LAST_ID_COLUMN = TABLE_PREFIX + JdbcSchemaManager.COLUMN_PART_SEP + "last_id";
  private static final String MODIFICATION_TIME_COLUMN = TABLE_PREFIX + JdbcSchemaManager.COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
  private static final String CREATION_TIME_COLUMN = TABLE_PREFIX + JdbcSchemaManager.COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  private static final String TABLE_NAME_COLUMN = TABLE_PREFIX + JdbcSchemaManager.COLUMN_PART_SEP + "table_name";
  private static final String REALM_ID_COLUMN = TABLE_PREFIX + JdbcSchemaManager.COLUMN_PART_SEP + "realm_id";


  public Future<Long> getNextIdForTableAndRealm(SqlConnection sqlConnection, Realm realm, JdbcTable table) {
    // not full table name because we may move them from schema
    String name = table.getName();
    return this.getNextIdForTableAndRealm(sqlConnection, realm, name);
  }


  /**
   * @param sqlConnection - the sql connection
   * @param realm         - the realm
   * @param tableName -the table name
   * @return the next id
   * @deprecated use {@link #getNextIdForTableAndRealm(SqlConnection, Realm, JdbcTable)}
   */
  @Deprecated
  public Future<Long> getNextIdForTableAndRealm(SqlConnection sqlConnection, Realm realm, String tableName) {

    Long realmId = realm.getLocalId();
    // https://vertx.io/docs/vertx-pg-client/java/#_returning_clauses
    String updateSql = "update " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n " +
      "set " +
      LAST_ID_COLUMN + " = " + LAST_ID_COLUMN + " + 1, \n" +
      MODIFICATION_TIME_COLUMN + " = $1 \n" +
      "where " + TABLE_NAME_COLUMN + " = $2 \n " +
      "and " + REALM_ID_COLUMN + " = $3 " +
      "RETURNING " + LAST_ID_COLUMN;
    return sqlConnection
      .preparedQuery(updateSql)
      .execute(Tuple.of(
        DateTimeService.getNowInUtc(),
        tableName,
        realmId
      ))
      .recover(t -> Future.failedFuture(
        TowerFailureException.builder()
          .setMessage("Error while updating the sequence for the table (" + tableName + ") with the the following sql:\n" + updateSql)
          .setCauseException(t)
          .build())
      )
      .compose(rows -> {
        if (rows.size() == 0) {
          // insert
          Long startId = 1L;
          String insertSql = "insert into " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " (\n " +
            REALM_ID_COLUMN + ",\n" +
            TABLE_NAME_COLUMN + ",\n" +
            LAST_ID_COLUMN + ",\n" +
            CREATION_TIME_COLUMN + "\n" +
            ") values ($1, $2, $3, $4)";
          Tuple tuple = Tuple.of(
            realmId,
            tableName,
            startId,
            DateTimeService.getNowInUtc()
          );
          return sqlConnection.preparedQuery(insertSql)
            .execute(tuple)
            .recover(t -> Future.failedFuture(
              TowerFailureException.builder()
                .setMessage("Error while creating the sequence for the the table (" + tableName + ") the following sql:\n" + insertSql + "\n")
                .setCauseException(t)
                .build())
            )
            .compose(ok -> Future.succeededFuture(startId));
        }
        if (rows.size() > 1) {
          return Future.failedFuture("realm_sequence_id: The number of rows returned is bigger than 1 with the realm (" + realmId + ") and the table (" + tableName + ").");
        }
        Row row = rows.iterator().next();
        Long nextId = row.getLong(LAST_ID_COLUMN);
        return Future.succeededFuture(nextId);
      });
  }


}
