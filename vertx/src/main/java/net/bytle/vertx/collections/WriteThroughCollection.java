package net.bytle.vertx.collections;

import net.bytle.vertx.JdbcClient;
import net.bytle.vertx.JdbcSchema;
import net.bytle.vertx.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WriteThroughCollection {

  static Logger LOGGER = LogManager.getLogger(WriteThroughCollection.class);
  private final JdbcSchema schema;
  private final JdbcClient postgresClient;

  public WriteThroughCollection(Server server) {
    LOGGER.info("Write Through Collection Db Migration");
    postgresClient = server.getPostgresClient();

    schema = JdbcSchema.builder(postgresClient,"collection").build();

  }

  public <E> WriteThroughQueue<E> createQueue(Class<E> clazz, String queueName, WriteThroughElementSerializer<E> serializer) {

    return WriteThroughQueue.builder(this, clazz, queueName)
      .setSerializer(serializer)
      .build();

  }

  public JdbcClient getJdbcServer() {
    return this.postgresClient;
  }

  public JdbcSchema getJdbcSchema() {
    return schema;
  }

}
