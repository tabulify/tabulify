package net.bytle.vertx.collections;

import io.vertx.pgclient.PgPool;
import net.bytle.exception.DbMigrationException;
import net.bytle.exception.InternalException;
import net.bytle.vertx.JdbcConnectionInfo;
import net.bytle.vertx.JdbcSchema;
import net.bytle.vertx.JdbcSchemaManager;
import net.bytle.vertx.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WriteThroughCollection {

  static Logger LOGGER = LogManager.getLogger(WriteThroughCollection.class);
  private final PgPool pool;
  private final String schema;

  public WriteThroughCollection(Server server) {
    LOGGER.info("Write Through Collection Db Migration");
    JdbcConnectionInfo postgresDatabaseConnectionInfo = server.getPostgresDatabaseConnectionInfo();
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
    this.pool = server.getPostgresDatabaseConnectionPool();
  }

  public <E> WriteThroughQueue<E> createQueue(Class<E> clazz, String queueName, WriteThroughElementSerializer<E> serializer) {

    return WriteThroughQueue.builder(this, clazz, queueName)
      .setSerializer(serializer)
      .build();

  }

  public PgPool getPool() {
    return this.pool;
  }

  public String getTableSchema() {
      return schema;
  }

}
