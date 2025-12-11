package com.tabulify.postgres;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttributeEnumBase;
import com.tabulify.docker.DockerService;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlConnectionProvider;
import com.tabulify.service.Service;
import com.tabulify.service.ServiceAttributeBase;
import com.tabulify.type.KeyNormalizer;

import java.util.Map;
import java.util.Set;

import static com.tabulify.jdbc.SqlConnectionAttributeEnum.DRIVER;

public class PostgresProvider extends SqlConnectionProvider {

  /**
   * This is the shorter name (used in the docker image)
   */
  public static final KeyNormalizer HOWTO_POSTGRES_NAME = KeyNormalizer.createSafe("postgres");
  /**
   * This is the shorter name (used by jdbc)
   */
  public static final String POSTGRESQL = HOWTO_POSTGRES_NAME + "ql";
  public static final Integer HOWTO_PORT = 5432;
  public static final String HOWTO_PASSWORD = "welcome";
  private static final String HOWTO_USER = "postgres";
  public final String URL_PREFIX = "jdbc:" + POSTGRESQL + ":";

  @Override
  public Connection createSqlConnection(Tabular tabular, Attribute name, Attribute url) {
    return new PostgresConnection(tabular, name, url);
  }

  @Override
  public boolean accept(Attribute url) {
    return url.getValueOrDefaultAsStringNotNull().startsWith(URL_PREFIX);
  }

  @Override
  public Set<SqlConnection> getHowToConnections(Tabular tabular) {
    // jdbc:postgresql://host:port/database?prop=value
    Attribute name = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.NAME, HOWTO_POSTGRES_NAME, Origin.DEFAULT);
    Attribute uri = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.URI, URL_PREFIX + "//localhost:" + HOWTO_PORT + "/" + HOWTO_POSTGRES_NAME, Origin.DEFAULT);
    return Set.of(
      (SqlConnection) new PostgresConnection(tabular, name, uri)
        .setComment("The howto " + HOWTO_POSTGRES_NAME + " connection")
        .addAttribute(DRIVER, "org.postgresql.Driver", Origin.DEFAULT)
        .setUser(HOWTO_USER)
        .setPassword(HOWTO_PASSWORD)
    );
  }

  @Override
  public Set<Service> getHowToServices(Tabular tabular) {
    Attribute name = tabular.getVault().createAttribute(ServiceAttributeBase.NAME, HOWTO_POSTGRES_NAME, Origin.DEFAULT);
    return Set.of(
      new DockerService(tabular, name)
        .setPorts(Map.of(HOWTO_PORT, HOWTO_PORT))
        .setEnvs(Map.of("POSTGRES_PASSWORD", HOWTO_PASSWORD))
        .setImage(HOWTO_POSTGRES_NAME + ":13.21")
    );

  }
}
