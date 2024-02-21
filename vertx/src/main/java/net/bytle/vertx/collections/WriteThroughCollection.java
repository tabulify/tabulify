package net.bytle.vertx.collections;

import net.bytle.exception.DbMigrationException;
import net.bytle.exception.InternalException;
import net.bytle.vertx.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WriteThroughCollection {

  static Logger LOGGER = LogManager.getLogger(WriteThroughCollection.class);
  private final String schema;
  private final JdbcClient postgresServer;

  public WriteThroughCollection(Server server) {
    LOGGER.info("Write Through Collection Db Migration");
    postgresServer = server.getPostgresClient();
    JdbcConnectionInfo postgresDatabaseConnectionInfo = postgresServer.getConnectionInfo();
    JdbcSchemaManager jdbcSchemaManager = JdbcSchemaManager.create(postgresDatabaseConnectionInfo);
    schema = JdbcSchemaManager.getSchemaFromHandle("collection");
    JdbcSchema realmSchema = JdbcSchema.builder()
      .setLocation("classpath:db/cs-collection")
      .setSchema(schema)
      .build();
    try {
      jdbcSchemaManager.migrate(realmSchema);
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

  public String getTableSchema() {
      return schema;
  }

}
