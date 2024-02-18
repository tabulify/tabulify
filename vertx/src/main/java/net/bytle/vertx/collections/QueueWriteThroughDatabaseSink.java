package net.bytle.vertx.collections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
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
public class QueueWriteThroughDatabaseSink {

  private final Logger LOGGER = LogManager.getLogger(QueueWriteThroughDatabaseSink.class);

  private final PgPool pool;
  private final String queueName;
  private final String deleteSql;
  private final String clearSql;
  private final String addSql;
  private final JsonMapper mapper;
  private final String removeHeadSql;

  public <E extends CollectionWriteThroughElement> QueueWriteThroughDatabaseSink(QueueWriteThrough.Builder<E> queuBuilder) {
    this.queueName = queuBuilder.queueName;
    this.pool = queuBuilder.pool;
    String queueTable = "cs_runtime.queues";
    deleteSql = "delete from " + queueTable + " where queue_name = $1 and object_id = $2";
    clearSql = "delete from " + queueTable + " where queue_name = $1";
    addSql = "insert into " + queueTable + " (queue_name, object_id, creation_time, data) values($1, $2, $3, $4)";
    removeHeadSql = "insert into " + queueTable + " (queue_name, hash_code, to_string, creation_time, data) values($1, $2, $3, $4, $5)";
    mapper = queuBuilder.mapper;
  }



  public void remove(Object o) {

    String objectId = ((CollectionWriteThroughElement) o).getObjectId();
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
  public <E extends CollectionWriteThroughElement> void addToTail(E e) {
    String data;
    try {
      data = this.mapper.writeValueAsString(e.toJacksonObject());
    } catch (JsonProcessingException ex) {
      LOGGER.error("Unable to write the value (" + e + ") as jackson json string", ex);
      return;
    }
    Tuple tuple = Tuple.of(
      this.queueName,
      e.getObjectId(),
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
    // TODO: create the sql based on the timestamp
    // this.pool.query(removeHeadSql)
    // .execute()
    // .onFailure(err -> LOGGER.error("Unable to clear with the SQL\n" + clearSql, err));
  }
}
