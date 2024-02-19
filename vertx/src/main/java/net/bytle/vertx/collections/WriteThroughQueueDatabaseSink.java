package net.bytle.vertx.collections;

import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.CastException;
import net.bytle.vertx.DateTimeUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;

/**
 * The database sink for a {@link WriteThroughQueue}.
 * The objects are backed in the database with
 * - the id
 * - the data in string
 */
public class WriteThroughQueueDatabaseSink<E> {


  private final Logger LOGGER = LogManager.getLogger(WriteThroughQueueDatabaseSink.class);

  private final PgPool pool;
  private final String queueName;
  private final String deleteSql;
  private final String clearSql;
  private final String addSql;

  private final String removeHeadElementSql;

  final static String queueTable = "cs_runtime.queues";
  private final WriteThroughElementSerializer<E> serializer;
  private final String initSelectAllSql;
  private final String OBJECT_ID_COLUMN = "object_id";
  private static final String DATA_COLUMN = "data";

  public WriteThroughQueueDatabaseSink(WriteThroughQueue.Builder<E> queueBuilder) {
    this.queueName = queueBuilder.queueName;
    serializer = queueBuilder.serializer;
    this.pool = queueBuilder.pool;
    deleteSql = "delete from " + queueTable + " where queue_name = $1 and object_id = $2";
    clearSql = "delete from " + queueTable + " where queue_name = $1";
    addSql = "insert into " + queueTable + " (queue_name, " + OBJECT_ID_COLUMN + ", creation_time, " + DATA_COLUMN + ") values($1, $2, $3, $4)";
    /**
     * Remove the Head element
     * This Sql removes by time asc
     * The latest element is removed.
     */
    removeHeadElementSql = "delete\n" +
      "from " + queueTable + "\n" +
      "where object_id = (select object_id\n" +
      "                   from (select row_number() over (order by creation_time asc) as row_number, object_id\n" +
      "                         from " + queueTable + "\n" +
      "                         where queue_name = $1) queue_with_row_number\n" +
      "                   where row_number = 1)";
    /**
     * The select to build/init the queue back from the database
     * We sort by time asc so that the older elements are first
     */
    initSelectAllSql = "select * from " + queueTable + " where queue_name = $1 order by creation_time asc";
  }


  public void remove(Object o) {

    @SuppressWarnings("unchecked")
    String objectId = this.serializer.getStoreId((E) o);
    this.pool.preparedQuery(deleteSql)
      .execute(
        Tuple.of(
          this.queueName,
          objectId
        )
      )
      .onFailure(err -> LOGGER.error("Unable to delete with the SQL\n" + deleteSql, err));
  }

  public void clear() {
    this.pool.preparedQuery(clearSql)
      .execute(
        Tuple.of(
          this.queueName
        )
      )
      .onFailure(err -> LOGGER.error("Unable to clear with the SQL\n" + clearSql, err));
  }

  /**
   * Add at the tail
   */
  public void addToTail(E e) {
    String data;
    try {
      data = this.serializer.serialize(e);
    } catch (CastException ex) {
      LOGGER.error("Fatal Error in serialization of (" + e + ")", ex);
      return;
    }
    String objectId = this.serializer.getStoreId(e);
    Tuple tuple = Tuple.of(
      this.queueName,
      objectId,
      DateTimeUtil.getNowInUtc(),
      data
    );
    this.pool.preparedQuery(addSql)
      .execute(tuple)
      .onFailure(err -> LOGGER.error("Unable to add with the SQL\n" + addSql, err));
  }

  /**
   * Remove the head
   */
  public void removeHead() {
    this.pool.preparedQuery(removeHeadElementSql)
      .execute(
        Tuple.of(
          this.queueName
        )
      )
      .onFailure(err -> LOGGER.error("Unable to remove head with the SQL\n" + removeHeadElementSql, err));
  }

  /**
   * @param queue - the in-memory queue to sync with the store
   */
  public void initAll(LinkedList<E> queue) {
    this.pool.preparedQuery(initSelectAllSql)
      .execute(
        Tuple.of(
          this.queueName
        )
      )
      .onFailure(err -> LOGGER.error("Unable to select all element with the SQL\n" + initSelectAllSql, err))
      .onSuccess(rows -> {
        if(rows.size()==0){
          LOGGER.info("No element to load back in the queue ("+this.queueName+")");
          return;
        } else {
          LOGGER.info("Loading ("+rows.size()+") elements in the queue ("+this.queueName+")");
        }
        for (Row row : rows) {
          String data = row.getString(DATA_COLUMN);
          String id = row.getString(OBJECT_ID_COLUMN);
          E element;
          try {
            element = this.serializer.deserialize(data);
          } catch (CastException e) {
            LOGGER.error("The element (" + id + ") from the queue (" + this.queueName + ") could not be build back from its database string representation. Error: " + e.getMessage() + ".\n Data: " + data, e);
            continue;
          }
          this.serializer.setStoreId(element, id);
          queue.add(element);
        }
        LOGGER.info("("+rows.size()+") elements were loaded in the queue ("+this.queueName+")");
      });
  }
}
