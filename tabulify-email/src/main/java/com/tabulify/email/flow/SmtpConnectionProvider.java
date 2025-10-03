package com.tabulify.email.flow;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttributeEnumBase;
import com.tabulify.connection.ObjectOrigin;
import com.tabulify.docker.DockerService;
import com.tabulify.service.Service;
import com.tabulify.service.ServiceAttributeBase;
import com.tabulify.spi.ConnectionProvider;
import net.bytle.exception.IllegalStructure;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.UriEnhanced;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SmtpConnectionProvider extends ConnectionProvider {

  /**
   * The default connection name
   * We choose smtp and not email
   * because email is a more common word
   * and may clash if a user uses it as connection name
   */
  public static final KeyNormalizer DEFAULT_SMTP_CONNECTION_NAME = KeyNormalizer.createSafe("smtp");
  public static final KeyNormalizer HOWTO_SMTP_CONNECTION_NAME = DEFAULT_SMTP_CONNECTION_NAME;

  public static final String SMTP_SCHEME = "smtp";
  public static final int MAILPIT_PORT = 1025;


  @Override
  public Connection createConnection(Tabular tabular, Attribute name, Attribute uri) {
    return new SmtpConnection(tabular, name, uri);
  }

  @Override
  public boolean accept(Attribute uri) {
    return uri.getValueOrDefaultAsStringNotNull().startsWith(SMTP_SCHEME);
  }

  @Override
  public Set<Connection> getHowToConnections(Tabular tabular) {

    UriEnhanced smtpUri;
    try {
      smtpUri = UriEnhanced.create()
        .setScheme(SMTP_SCHEME)
        .setHost("localhost")
        .setPort(MAILPIT_PORT);
    } catch (IllegalStructure e) {
      throw new RuntimeException(e);
    }
    Attribute name = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.NAME, HOWTO_SMTP_CONNECTION_NAME, Origin.DEFAULT);
    Attribute uri = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.URI, smtpUri.toUri().toString(), Origin.DEFAULT);

    Connection smtpConnection = new SmtpConnection(tabular, name, uri)
      // to is mandatory in dev
      .addAttribute(SmtpConnectionAttributeEnum.TO, "support@tabulify.com", Origin.DEFAULT)
      // from so that in the email, we don't see the computer name
      .addAttribute(SmtpConnectionAttributeEnum.FROM, "support@tabulify.com", Origin.DEFAULT)
      .setComment("HowTo Smtp Connection")
      .setOrigin(ObjectOrigin.BUILT_IN);
    return Set.of(smtpConnection);

  }

  @Override
  public Set<Service> getHowToServices(Tabular tabular) {
    Attribute name = tabular.getVault().createAttribute(ServiceAttributeBase.NAME, HOWTO_SMTP_CONNECTION_NAME, Origin.DEFAULT);
    // https://hub.docker.com/r/axllent/mailpit/tags
    String version = "v1.25.1";
    // https://mailpit.axllent.org/docs/configuration/runtime-options/
    Map<String, String> envs = new HashMap<>();
    envs.put("MP_LABEL", "Tabulify HowTo Service");
    envs.put("MP_TENANT_ID", "tabulify");
    // Ports
    // 1025 for SMTP
    // 8025 for the web interface.
    Map<Integer, Integer> ports = new HashMap<>();
    ports.put(MAILPIT_PORT, MAILPIT_PORT);
    ports.put(8025, 8025);
    return Set.of(
      new DockerService(tabular, name)
        .setImage("axllent/mailpit:" + version)
        .setPorts(ports)
        .setEnvs(envs)
    );
  }

}
