package com.tabulify.oracle;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.tabulify.jdbc.SqlConnectionAttributeEnum.DRIVER;

public class OracleProvider extends SqlConnectionProvider {

  public static final KeyNormalizer ORACLE_HOWTO_NAME = KeyNormalizer.createSafe("oracle");
  public static final String HOWTO_CONNECTION_PASSWORD = "oracle";
  public static final String HOWTO_CONNECTION_USER = "system";
  public static final String HOWTO_CONNECTION_SCHEMA = "tabulify";
  public static final Logger ORACLE_LOGGER = Logger.getLogger("oracle.jdbc");


  public OracleProvider() {
    /**
     * To avoid
     * Dec 10, 2020 1:23:32 PM oracle.jdbc.driver.OracleDriver registerMBeans
     * WARNING: Error while registering Oracle JDBC Diagnosability MBean.
     * java.security.AccessControlException: access denied ("javax.management.MBeanTrustPermission" "register")
     * 	at java.security.AccessControlContext.checkPermission(AccessControlContext.java:472)
     * 	at java.lang.SecurityManager.checkPermission(SecurityManager.java:585)
     * 	at com.sun.jmx.interceptor.DefaultMBeanServerInterceptor.checkMBeanTrustPermission(DefaultMBeanServerInterceptor.java:1848)
     * 	at com.sun.jmx.interceptor.DefaultMBeanServerInterceptor.registerMBean(DefaultMBeanServerInterceptor.java:322)
     * 	at com.sun.jmx.mbeanserver.JmxMBeanServer.registerMBean(JmxMBeanServer.java:522)
     * <p>
     * Severe needs to be set from a static variable otherwise it's deleted
     * drainLoggerRefQueueBounded method in java.util.logging.LogManager cleans up logger references that are no longer strongly referenced, which can reset your logger configuration.
     */
    ORACLE_LOGGER.setLevel(Level.SEVERE);
  }

  @Override
  public Connection createSqlConnection(Tabular tabular, Attribute name, Attribute url) {
    return new OracleConnection(tabular, name, url);
  }

  @Override
  public boolean accept(Attribute url) {
    return url.getValueOrDefaultAsStringNotNull().contains("oracle");
  }


  @Override
  public Set<SqlConnection> getHowToConnections(Tabular tabular) {
    Attribute name = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.NAME, ORACLE_HOWTO_NAME, Origin.DEFAULT);
    Attribute uri = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.URI, "jdbc:oracle:thin:@localhost:1521/freepdb1", Origin.DEFAULT);
    return Set.of(
      (SqlConnection) new OracleConnection(tabular, name, uri)
        // in idea, you can set this statement in startup script
        .setLoginStatements("ALTER SESSION SET CURRENT_SCHEMA = " + HOWTO_CONNECTION_SCHEMA)
        .setComment("The howto oracle connection")
        .setUser(HOWTO_CONNECTION_USER)
        .setPassword(HOWTO_CONNECTION_PASSWORD)
        .addAttribute(DRIVER, "oracle.jdbc.OracleDriver", Origin.DEFAULT)
    );
  }

  @Override
  public Set<Service> getHowToServices(Tabular tabular) {
    Attribute name = tabular.getVault().createAttribute(ServiceAttributeBase.NAME, ORACLE_HOWTO_NAME, Origin.DEFAULT);
    Map<String, String> envs = new HashMap<>();
    envs.put("ORACLE_DISABLE_ASYNCH_IO", "true");
    envs.put("ORACLE_ALLOW_REMOTE", "true");
    envs.put("ORACLE_PASSWORD", HOWTO_CONNECTION_PASSWORD);
    // Schema
    envs.put("APP_USER", HOWTO_CONNECTION_SCHEMA);
    envs.put("APP_USER_PASSWORD", HOWTO_CONNECTION_PASSWORD);
    return Set.of(
      new DockerService(tabular, name)
        .setImage("ghcr.io/gvenzl/oracle-free:23.7-slim-faststart")
        .setPorts(Map.of(1521, 1521))
        .setEnvs(envs)
    );
  }
}
