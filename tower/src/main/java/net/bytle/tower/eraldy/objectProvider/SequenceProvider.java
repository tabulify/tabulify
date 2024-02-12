package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.vertx.DateTimeUtil;
import net.bytle.vertx.JdbcSchemaManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SequenceProvider {


  private static final Logger LOGGER = LogManager.getLogger(SequenceProvider.class);
  private static final String TABLE_NAME = "realm_sequence";

  private static final String TABLE_PREFIX = "sequence";
  private static final String LAST_ID_COLUMN = TABLE_PREFIX + JdbcSchemaManager.COLUMN_PART_SEP + "last_id";
  private static final String MODIFICATION_TIME_COLUMN = TABLE_PREFIX + JdbcSchemaManager.COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
  private static final String CREATION_TIME_COLUMN = TABLE_PREFIX + JdbcSchemaManager.COLUMN_PART_SEP + JdbcSchemaManager.CREATION_TIME_COLUMN_SUFFIX;
  private static final String TABLE_NAME_COLUMN = TABLE_PREFIX + JdbcSchemaManager.COLUMN_PART_SEP + "table_name";
  private static final String REALM_ID_COLUMN = TABLE_PREFIX + JdbcSchemaManager.COLUMN_PART_SEP + RealmProvider.ID_COLUMN;


  public static Future<Long> getNextIdForTableAndRealm(
    SqlConnection sqlConnection, String tableName, Realm realm) {

    Long realmId = realm.getLocalId();
    // https://vertx.io/docs/vertx-pg-client/java/#_returning_clauses
    String updateSql = "update " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + "\n " +
      "set " +
      LAST_ID_COLUMN + " = " + LAST_ID_COLUMN + " + 1, \n" +
      MODIFICATION_TIME_COLUMN + " = $1 \n" +
      "where " + TABLE_NAME_COLUMN + " = $2 \n " +
      "and " + REALM_ID_COLUMN + " = $3 " +
      "RETURNING " + LAST_ID_COLUMN;
    return sqlConnection.preparedQuery(updateSql)
      .execute(Tuple.of(
        DateTimeUtil.getNowInUtc(),
        tableName,
        realmId
      ))
      .onFailure(t -> LOGGER.error("Error while executing the following sql:\n" + updateSql, t))
      .compose(rows -> {
        Long nextId;
        if (rows.size() == 0) {
          // insert
          nextId = 1L;
          String insertSql = "insert into " + JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME + " (\n " +
            REALM_ID_COLUMN + ",\n" +
            TABLE_NAME_COLUMN + ",\n" +
            LAST_ID_COLUMN + ",\n" +
            CREATION_TIME_COLUMN + "\n" +
            ") values ($1, $2, $3, $4)";
          return sqlConnection.preparedQuery(
              insertSql)
            .execute(Tuple.of(
              realmId,
              tableName,
              nextId,
              DateTimeUtil.getNowInUtc()
            ))
            .onFailure(t -> LOGGER.error("Error while executing the following sql:\n" + insertSql, t))
            .compose(ok -> Future.succeededFuture(nextId));
        }
        if (rows.size() > 1) {
          return Future.failedFuture("realm_sequence_id: The number of rows returned is bigger than 1 with the realm (" + realmId + ") and the table (" + tableName + ").");
        }
        Row row = rows.iterator().next();
        nextId = row.getLong(LAST_ID_COLUMN);
        return Future.succeededFuture(nextId);
      });
  }
}
