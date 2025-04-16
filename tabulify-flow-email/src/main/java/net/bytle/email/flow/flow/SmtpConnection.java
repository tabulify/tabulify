package net.bytle.email.flow.flow;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttribute;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataSystem;
import com.tabulify.spi.ProcessingEngine;
import net.bytle.email.BMailAddressStatic;
import net.bytle.email.BMailSmtpClient;
import net.bytle.email.BMailSmtpConnectionAttribute;
import net.bytle.email.BMailStartTls;
import net.bytle.exception.*;
import net.bytle.os.Oss;
import net.bytle.type.*;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SmtpConnection extends Connection {


  private final UriEnhanced uri;
  private BMailSmtpClient smtpServer;

  private List<InternetAddress> defaultTo = new ArrayList<>();
  private List<InternetAddress> defaultCc = new ArrayList<>();
  private List<InternetAddress> defaultBcc = new ArrayList<>();
  private InternetAddress defaultFrom;

  public SmtpConnection(Tabular tabular, Variable name, Variable uri) {

    super(tabular, name, uri);
    String uriValue = uri.getValueOrDefaultAsStringNotNull();
    try {
      this.uri = UriEnhanced.createFromString(uriValue);
    } catch (IllegalStructure e) {
      throw new IllegalArgumentException("The smtp connection (" + name + ") has an illegal URI value (" + uriValue + "). Error: " + e.getMessage(), e);
    }

    this.addVariablesFromEnumAttributeClass(BMailSmtpConnectionAttribute.class);

    this.buildDefault();


  }

  @Override
  public Connection addVariable(String key, Object value) {

    BMailSmtpConnectionAttribute BMailSmtpConnectionAttribute;
    try {
      BMailSmtpConnectionAttribute = Casts.cast(key, BMailSmtpConnectionAttribute.class);
    } catch (Exception e) {
      return super.addVariable(key, value);
    }

    try {
      Variable variable = getTabular().createVariable(BMailSmtpConnectionAttribute, value);
      this.addVariable(variable);
      return this;
    } catch (Exception e) {
      throw new RuntimeException("Error while creating the variable (" + BMailSmtpConnectionAttribute + ") with the value (" + value + ") for the connection (" + this + ")", e);
    }

  }

  private void buildDefault() {

    try {
      this.defaultFrom = BMailAddressStatic.addressAndNameToInternetAddress(
        this.getDefaultFromProperty(),
        this.getDefaultFromNameProperty()
      );
    } catch (AddressException | UnsupportedEncodingException e) {
      throw new RuntimeException("Error on the `from` definition of the smtp connection with the value ("+this.getDefaultFromProperty()+"). Error: " + e.getMessage(),e);
    }

    try {
      this.defaultTo = BMailAddressStatic.addressAndNamesToListInternetAddress(
        this.getQueryPropertyOrConnectionPropertyOrNull(BMailSmtpConnectionAttribute.TO.toString()),
        this.getQueryPropertyOrConnectionPropertyOrNull(BMailSmtpConnectionAttribute.TO_NAMES.toString())
      );
    } catch (AddressException | UnsupportedEncodingException e) {
      throw new RuntimeException("Error on the `to` definition of the smtp connection. Error: " + e.getMessage());
    }

    try {
      this.defaultCc = BMailAddressStatic.addressAndNamesToListInternetAddress(
        this.getQueryPropertyOrConnectionPropertyOrNull(BMailSmtpConnectionAttribute.CC.toString()),
        this.getQueryPropertyOrConnectionPropertyOrNull(BMailSmtpConnectionAttribute.CC_NAMES.toString())
      );
    } catch (AddressException | UnsupportedEncodingException e) {
      throw new RuntimeException("Error on the `cc` definition of the smtp connection. Error: " + e.getMessage());
    }

    try {
      this.defaultBcc = BMailAddressStatic.addressAndNamesToListInternetAddress(
        this.getQueryPropertyOrConnectionPropertyOrNull(BMailSmtpConnectionAttribute.BCC.toString()),
        this.getQueryPropertyOrConnectionPropertyOrNull(BMailSmtpConnectionAttribute.BCC_NAMES.toString())
      );
    } catch (AddressException | UnsupportedEncodingException e) {
      throw new RuntimeException("Error on the `cc` definition of the smtp connection. Error: " + e.getMessage());
    }
  }

  private Boolean getTls() {
    try {
      return getBooleanProperty(BMailSmtpConnectionAttribute.TLS.toString());
    } catch (NoValueException | NoVariableException e) {
      return BMailSmtpConnectionAttribute.DEFAULTS.TLS;
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createFromMessage("The value for the attribute (" + BMailSmtpConnectionAttribute.AUTH + ") of the connection (" + this + ") is not valid. Error: " + e.getMessage(), e);
    }
  }

  private Boolean getDebug() {

    try {
      return getBooleanProperty(BMailSmtpConnectionAttribute.DEBUG.toString());
    } catch (NoValueException | NoVariableException e) {
      return false;
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createFromMessage("The value for the attribute (" + BMailSmtpConnectionAttribute.DEBUG + ") of the connection (" + this + ") is not valid. Error: " + e.getMessage(), e);
    }

  }

  @Nullable
  private Boolean getBooleanProperty(String propertyKey) throws NoValueException, NoVariableException, CastException {
    String tls = this.uri.getQueryProperty(propertyKey);
    if (tls != null && !tls.trim().isEmpty()) {
      return Booleans.createFromString(tls).toBoolean();
    }
    return this.getVariable(propertyKey).getValueOrDefaultCastAs(Boolean.class);
  }

  private String getSmtpPassword() throws NoValueException {
    String password = this.uri.getQueryProperty(ConnectionAttribute.PASSWORD);
    if (password != null) {
      return password;
    }
    return (String) this.getPasswordVariable().getValueOrDefault();

  }

  private String getSmtpUser() throws NoValueException {

    String user = this.uri.getQueryProperty(ConnectionAttribute.USER);
    if (user != null && !user.trim().isEmpty()) {
      return user;
    }
    return (String) this.getUser().getValueOrDefault();


  }

  private Integer getPort() {

    Integer port = this.uri.getPort();
    if (port != null) {
      return port;
    }
    try {
      return (Integer) this.getVariable(BMailSmtpConnectionAttribute.PORT).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      return BMailSmtpConnectionAttribute.DEFAULTS.PORT;
    }


  }

  @Override
  public DataSystem getDataSystem() {
    throw new RuntimeException("The smtp does not have any data system implemented");
  }

  @Override
  public DataPath getDataPath(String pathOrName, MediaType mediaType) {
    throw new RuntimeException("The smtp does not have any data system implemented");
  }

  @Override
  public DataPath getDataPath(String pathOrName) {
    throw new RuntimeException("The smtp does not have any data system implemented");
  }

  @Override
  public String getCurrentPathCharacters() {
    throw new RuntimeException("The smtp does not have any data system implemented");
  }

  @Override
  public String getParentPathCharacters() {
    throw new RuntimeException("The smtp does not have any data system implemented");
  }

  @Override
  public String getSeparator() {
    throw new RuntimeException("The smtp does not have any data system implemented");
  }

  @Override
  public DataPath getCurrentDataPath() {
    throw new RuntimeException("The smtp does not have any data system implemented");
  }


  @Override
  public DataPath createScriptDataPath(DataPath dataPath) {
    throw new RuntimeException("Smtp does not support scripting");
  }

  @Override
  public ProcessingEngine getProcessingEngine() {
    throw new RuntimeException("The smtp does not have any data system implemented");
  }

  @Override
  public Boolean ping() {
    try {
      this.getSmtpServer().pingHello();
      return false;
    } catch (MessagingException e) {
      return true;
    }
  }

  public BMailSmtpClient getSmtpServer() {

    if (smtpServer != null) {
      return smtpServer;
    }

    /*
     * We build it late because the username and the
     * password may be passed later at build time.
     * Bad yeah.
     */
    BMailSmtpClient.config smtpConfig = BMailSmtpClient.create();

    smtpConfig.setHost(this.getHost());

    Integer port = this.getPort();
    smtpConfig.setPort(port);

    try {
      String userName = this.getSmtpUser();
      smtpConfig.setUsername(userName);
    } catch (NoValueException e) {
      // ok
    }

    try {
      String userPassword = this.getSmtpPassword();
      smtpConfig.setPassword(userPassword);
    } catch (NoValueException e) {
      // ok
    }

    Boolean tls = this.getTls();
    if (tls) {
      smtpConfig.setStartTls(BMailStartTls.REQUIRE);
    }

    Boolean debug = this.getDebug();
    smtpConfig.setDebug(debug);

    this.smtpServer = smtpConfig.build();
    return smtpServer;

  }

  private String getHost() {

    try {
      return this.uri.getHost().toStringWithoutRoot();
    } catch (NotFoundException e) {
      // no host
    }
    try {
      return (String) this.getVariable(BMailSmtpConnectionAttribute.HOST).getValueOrDefaultOrNull();
    } catch (NoVariableException e) {
      return BMailSmtpConnectionAttribute.DEFAULTS.HOST;
    }


  }

  private String getDefaultFromProperty() {

    try {
      return Oss.getUser() + "@" + Oss.getFqdn().toStringWithoutRoot();
    } catch (UnknownHostException e) {
      return "tabulify@localhost";
    }
  }

  /**
   * To inject for test
   *
   * @param smtpServer - inject the smtp server
   * @return the object for chaining
   */
  public SmtpConnection setSmtpServer(BMailSmtpClient smtpServer) {
    this.smtpServer = smtpServer;
    return this;
  }


  private String getDefaultFromNameProperty() {
    String from = this.uri.getQueryProperty(BMailSmtpConnectionAttribute.FROM.toString().toLowerCase(Locale.ROOT));
    if (from != null) {
      return from;
    }

    try {
      return (String) this.getVariable(BMailSmtpConnectionAttribute.FROM).getValueOrDefaultOrNull();
    } catch (NoVariableException e) {
      return this.getDefaultFromProperty();
    }

  }

  private String getQueryPropertyOrConnectionPropertyOrNull(String name) {
    String from = this.uri.getQueryProperty(name);
    if (from != null) {
      return from;
    }
    try {
      return (String) this.getVariable(name).getValueOrDefaultOrNull();
    } catch (NoVariableException e) {
      return null;
    }
  }


  public List<InternetAddress> getDefaultToInternetAddresses() {
    return this.defaultTo;
  }

  public List<InternetAddress> getDefaultCcInternetAddresses() {
    return this.defaultCc;
  }

  public List<InternetAddress> getDefaultBccInternetAddresses() {
    return this.defaultBcc;
  }

  public SmtpConnection setDefaultTo(String addressList) {
    try {
      this.defaultTo = Arrays.asList(InternetAddress.parse(addressList));
    } catch (AddressException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public InternetAddress getDefaultFromInternetAddress() {
    return this.defaultFrom;
  }
}
