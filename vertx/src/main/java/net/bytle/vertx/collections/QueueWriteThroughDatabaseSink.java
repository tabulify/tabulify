package net.bytle.vertx.collections;

import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.CastException;
import net.bytle.vertx.DateTimeUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A queue cache backed by a store (database)
 * The object are backed in the database with
 * - the id being the output of the hashCode
 * - the name being the output of the toString
 * - the data being the json Jackson representation
 */
public class QueueWriteThroughDatabaseSink<E> {

  private final Logger LOGGER = LogManager.getLogger(QueueWriteThroughDatabaseSink.class);

  private final PgPool pool;
  private final String queueName;
  private final String deleteSql;
  private final String clearSql;
  private final String addSql;

  private final String removeHeadSql;

  final static String queueTable = "cs_runtime.queues";
  private final CollectionWriteThroughSerializer<E> mapper;

  public QueueWriteThroughDatabaseSink(QueueWriteThrough.Builder<E> queueBuilder) {
    this.queueName = queueBuilder.queueName;
    this.pool = queueBuilder.pool;
    deleteSql = "delete from " + queueTable + " where queue_name = $1 and object_id = $2";
    clearSql = "delete from " + queueTable + " where queue_name = $1";
    addSql = "insert into " + queueTable + " (queue_name, object_id, creation_time, data) values($1, $2, $3, $4)";
    removeHeadSql = "delete\n" +
      "from " + queueTable + "\n" +
      "where object_id = (select object_id\n" +
      "                   from (select row_number() over (order by creation_time asc) as row_number, object_id\n" +
      "                         from " + queueTable + "\n" +
      "                         where queue_name = $1) queue_with_row_number\n" +
      "                   where row_number = 1)";
    mapper = queueBuilder.serializer;
  }


  public void remove(Object o) {

    @SuppressWarnings("unchecked")
    String objectId = this.mapper.getObjectId((E) o);
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
      data = this.mapper.serialize(e);
    } catch (CastException ex) {
      LOGGER.error("Fatal Error in serialization of (" + e + ")", ex);
      return;
    }
    String objectId = this.mapper.getObjectId(e);
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
    this.pool.preparedQuery(removeHeadSql)
      .execute(
        Tuple.of(
          this.queueName
        )
      )
      .onFailure(err -> LOGGER.error("Unable to remove head with the SQL\n" + removeHeadSql, err));
  }
}
