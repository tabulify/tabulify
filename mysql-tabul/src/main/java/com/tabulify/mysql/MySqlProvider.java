package com.tabulify.mysql;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.connection.ConnectionAttributeEnumBase;
import com.tabulify.docker.DockerService;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlConnectionProvider;
import com.tabulify.service.Service;
import com.tabulify.service.ServiceAttributeBase;
import com.tabulify.type.KeyNormalizer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.tabulify.jdbc.SqlConnectionAttributeEnum.DRIVER;

public class MySqlProvider extends SqlConnectionProvider {

  public static final String HOWTO_DATABASE = "howto";
  public static final KeyNormalizer MYSQL_HOWTO_NAME = KeyNormalizer.createSafe("mysql");
  public static final String HOWTO_ROOT_PASSWORD = "my-secret-pw";
  private static final Integer HOWTO_PORT = 3306;
  public final String URL_PREFIX = "jdbc:" + MYSQL_HOWTO_NAME + ":";

  @Override
  public MySqlConnection createSqlConnection(Tabular tabular, Attribute name, Attribute url) {

    return new MySqlConnection(tabular, name, url);

  }

  @Override
  public boolean accept(Attribute url) {
    return url.getValueOrDefaultAsStringNotNull().startsWith(URL_PREFIX);
  }


  @Override
  public Set<SqlConnection> getHowToConnections(Tabular tabular) {

    /**
     * By default, my sql does not have any database
     * This is the one created in the test
     * {@link MySqlDataStoreResource}
     *
     * Documentation:
     * https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-jdbc-url-format.html
     * <p>
     * From the docker doc - https://hub.docker.com/_/mysql
     */
    Attribute name = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.NAME, MYSQL_HOWTO_NAME, Origin.DEFAULT);
    Attribute uri = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.URI, "jdbc:" + MYSQL_HOWTO_NAME + "://localhost:" + HOWTO_PORT + "/" + HOWTO_DATABASE, Origin.DEFAULT);
    return Set.of(
      (SqlConnection) new MySqlConnection(tabular, name, uri)
        .setComment("The default " + MYSQL_HOWTO_NAME + " data store")
        .addAttribute(DRIVER, "com." + MYSQL_HOWTO_NAME + ".cj.jdbc.Driver", Origin.DEFAULT)
        .setUser("root")
        .setPassword(HOWTO_ROOT_PASSWORD)
    );
  }

  @Override
  public Set<Service> getHowToServices(Tabular tabular) {
    Attribute name = tabular.getVault().createAttribute(ServiceAttributeBase.NAME, MYSQL_HOWTO_NAME, Origin.DEFAULT);
    Map<String, String> envs = new HashMap<>();
    envs.put("MYSQL_DATABASE", HOWTO_DATABASE);
    envs.put("MYSQL_ROOT_PASSWORD", HOWTO_ROOT_PASSWORD);
    return Set.of(new DockerService(tabular, name)
      .setPorts(Map.of(HOWTO_PORT, HOWTO_PORT))
      .setEnvs(envs)
      .setImage(MYSQL_HOWTO_NAME + ":5.7.37")
    );
  }
}
