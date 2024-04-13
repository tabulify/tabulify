package net.bytle.vertx.collections;

import net.bytle.exception.DbMigrationException;
import net.bytle.exception.InternalException;
import net.bytle.vertx.JdbcClient;
import net.bytle.vertx.JdbcSchema;
import net.bytle.vertx.JdbcSchemaManager;
import net.bytle.vertx.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WriteThroughCollection {

  static Logger LOGGER = LogManager.getLogger(WriteThroughCollection.class);
  private final JdbcSchema schema;
  private final JdbcClient postgresServer;

  public WriteThroughCollection(Server server) {
    LOGGER.info("Write Through Collection Db Migration");
    postgresServer = server.getPostgresClient();
    JdbcSchemaManager jdbcSchemaManager = postgresServer.getSchemaManager();

    schema = JdbcSchema.builder("collection").build();

    try {
      jdbcSchemaManager.migrate(schema);
    } catch (DbMigrationException e) {
      throw new InternalException("The Write Through Collection database migration failed", e);
    }

  }

  public <E> WriteThroughQueue<E> createQueue(Class<E> clazz, String queueName, WriteThroughElementSerializer<E> serializer) {

    return WriteThroughQueue.builder(this, clazz, queueName)
      .setSerializer(serializer)
      .build();

  }

  public JdbcClient getJdbcServer() {
    return this.postgresServer;
  }

  public JdbcSchema getJdbcSchema() {
    return schema;
  }

}
