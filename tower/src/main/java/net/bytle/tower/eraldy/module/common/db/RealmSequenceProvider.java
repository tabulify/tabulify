package net.bytle.tower.eraldy.module.common.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.db.*;

import java.time.LocalDateTime;

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


  private final JdbcTable seqTable;

  public RealmSequenceProvider(JdbcSchema jdbcSchema) {
    this.seqTable = JdbcTable.build(jdbcSchema, "realm_sequence", RealmSequenceCols.values())
      .addPrimaryKeyColumn(RealmSequenceCols.REALM_ID)
      .addPrimaryKeyColumn(RealmSequenceCols.TABLE_NAME)
      .build();
  }

  public Future<Long> getNextIdForTableAndRealm(SqlConnection sqlConnection, Realm realm, JdbcTable table) {
    // not full table name because we may move them from schema
    String tableIdInSequenceTable = table.getName();

    LocalDateTime nowInUtc = DateTimeService.getNowInUtc();
    return JdbcUpdate.into(this.seqTable)
      .setUpdatedColumnWithExpression(RealmSequenceCols.LAST_ID, RealmSequenceCols.LAST_ID.getColumnName() + " + 1")
      .setUpdatedColumnWithValue(RealmSequenceCols.MODIFICATION_TIME, nowInUtc)
      .addPredicateColumn(RealmSequenceCols.REALM_ID, realm.getGuid().getLocalId())
      .addPredicateColumn(RealmSequenceCols.TABLE_NAME, tableIdInSequenceTable)
      .addReturningColumn(RealmSequenceCols.LAST_ID)
      .execute(sqlConnection)
      .recover(t -> Future.failedFuture(
        TowerFailureException.builder()
          .setMessage("Error while updating the sequence for the table (" + tableIdInSequenceTable + ")")
          .setCauseException(t)
          .build())
      )
      .compose(rows -> {
        if (rows.size() == 0) {
          // insert
          Long startId = 1L;
          return JdbcInsert.into(this.seqTable)
            .addColumn(RealmSequenceCols.REALM_ID, realm.getGuid().getLocalId())
            .addColumn(RealmSequenceCols.TABLE_NAME, tableIdInSequenceTable)
            .addColumn(RealmSequenceCols.LAST_ID, startId)
            .addColumn(RealmSequenceCols.CREATION_TIME_COLUMN, nowInUtc)
            .addColumn(RealmSequenceCols.MODIFICATION_TIME, nowInUtc)
            .execute(sqlConnection)
            .recover(t -> Future.failedFuture(
              TowerFailureException.builder()
                .setMessage("Error while creating the sequence for the the table (" + tableIdInSequenceTable + ")")
                .setCauseException(t)
                .build())
            )
            .compose(ok -> Future.succeededFuture(startId));
        }
        if (rows.size() > 1) {
          return Future.failedFuture("realm_sequence_id: The number of rows returned is bigger than 1 with the realm (" + realm + ") and the table (" + tableIdInSequenceTable + ").");
        }
        JdbcRow row = rows.iterator().next();
        Long nextId = row.getLong(RealmSequenceCols.LAST_ID);
        return Future.succeededFuture(nextId);
      });
  }


}
