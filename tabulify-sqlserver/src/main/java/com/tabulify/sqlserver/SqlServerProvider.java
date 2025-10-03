package com.tabulify.sqlserver;

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
import net.bytle.type.KeyNormalizer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.tabulify.jdbc.SqlConnectionAttributeEnum.DRIVER;

public class SqlServerProvider extends SqlConnectionProvider {


  public static final KeyNormalizer HOWTO_SQLSERVER_NAME = KeyNormalizer.createSafe("sqlserver");
  public static final String URL_PREFIX = "jdbc:" + HOWTO_SQLSERVER_NAME + ":";
  public static final String HOWTO_CONNECTION_PASSWORD = "TheSecret1!";
  public static final String HOWTO_CONNECTION_USER = "sa";

  @Override
  public Connection createSqlConnection(Tabular tabular, Attribute name, Attribute url) {
    return new SqlServerConnection(tabular, name, url);
  }


  @Override
  public boolean accept(Attribute url) {
    return url.getValueOrDefaultAsStringNotNull().startsWith(URL_PREFIX);
  }

  @Override
  public Set<SqlConnection> getHowToConnections(Tabular tabular) {
    Attribute name = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.NAME, HOWTO_SQLSERVER_NAME, Origin.DEFAULT);
    Attribute uri = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.URI, "jdbc:sqlserver://localhost:1433;encrypt=true;trustServerCertificate=true", Origin.DEFAULT);
    return Set.of(
      (SqlConnection) new SqlServerConnection(tabular, name, uri)
        .setComment("The default sqlserver connection")
        .addAttribute(DRIVER, "com.microsoft.sqlserver.jdbc.SQLServerDriver", Origin.DEFAULT)
        .setUser(HOWTO_CONNECTION_USER)
        .setPassword(HOWTO_CONNECTION_PASSWORD)
    );
  }

  @Override
  public Set<Service> getHowToServices(Tabular tabular) {
    Attribute name = tabular.getVault().createAttribute(ServiceAttributeBase.NAME, HOWTO_SQLSERVER_NAME, Origin.DEFAULT);
    Map<String, String> envs = new HashMap<>();
    envs.put("ACCEPT_EULA", "Y");
    envs.put("MSSQL_SA_PASSWORD", HOWTO_CONNECTION_PASSWORD);
    /**
     * Note: CU means Cumulative Update
     */
    return Set.of(
      new DockerService(tabular, name)
        .setPorts(Map.of(1433, 1433))
        .setEnvs(envs)
        .setImage("mcr.microsoft.com/mssql/server:2022-CU19-ubuntu-22.04")
    );
  }

}
