package com.tabulify.email.flow;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttributeEnum;
import com.tabulify.connection.ConnectionAttributeEnumBase;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataSystem;
import com.tabulify.spi.ProcessingEngine;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import net.bytle.email.BMailAddressStatic;
import net.bytle.email.BMailSmtpClient;
import net.bytle.email.BMailStartTls;
import net.bytle.exception.NoValueException;
import net.bytle.os.Oss;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SmtpConnection extends Connection {


  private BMailSmtpClient smtpServer;

  private List<InternetAddress> defaultTo = new ArrayList<>();
  private List<InternetAddress> defaultCc = new ArrayList<>();
  private List<InternetAddress> defaultBcc = new ArrayList<>();
  private InternetAddress defaultFrom;

  public SmtpConnection(Tabular tabular, com.tabulify.conf.Attribute name, com.tabulify.conf.Attribute uri) {

    super(tabular, name, uri);


    this.addAttributesFromEnumAttributeClass(SmtpConnectionAttributeEnum.class);

    this.buildDefault();

  }

  @Override
  public Connection addAttribute(KeyNormalizer name, Object value, Origin origin) {

    SmtpConnectionAttributeEnum smtpConnectionAttribute;
    try {
      smtpConnectionAttribute = Casts.cast(name, SmtpConnectionAttributeEnum.class);
    } catch (Exception e) {
      return super.addAttribute(name, value, origin);
    }

    try {
      Attribute attribute = getTabular().getVault().createAttribute(smtpConnectionAttribute, value, origin);
      this.addAttribute(attribute);
      return this;
    } catch (Exception e) {
      throw new RuntimeException("Error while creating the variable (" + smtpConnectionAttribute + ") with the value (" + value + ") for the connection (" + this + ")", e);
    }

  }

  private void buildDefault() {

    try {
      this.defaultFrom = BMailAddressStatic.addressAndNameToInternetAddress(
        this.getDefaultFromProperty(),
        (String) this.getAttribute(SmtpConnectionAttributeEnum.FROM_NAME).getValueOrDefault()
      );
    } catch (AddressException | UnsupportedEncodingException e) {
      throw new RuntimeException("Error on the `from` definition of the smtp connection with the value (" + this.getDefaultFromProperty() + "). Error: " + e.getMessage(), e);
    }

    try {
      this.defaultTo = BMailAddressStatic.addressAndNamesToListInternetAddress(
        (String) this.getAttribute(SmtpConnectionAttributeEnum.TO).getValueOrDefault(),
        (String) this.getAttribute(SmtpConnectionAttributeEnum.TO_NAMES).getValueOrDefault()
      );
    } catch (AddressException | UnsupportedEncodingException e) {
      throw new RuntimeException("Error on the `to` definition of the smtp connection. Error: " + e.getMessage());
    }

    try {
      this.defaultCc = BMailAddressStatic.addressAndNamesToListInternetAddress(
        (String) this.getAttribute(SmtpConnectionAttributeEnum.CC).getValueOrDefault(),
        (String) this.getAttribute(SmtpConnectionAttributeEnum.CC_NAMES).getValueOrDefault()
      );
    } catch (AddressException | UnsupportedEncodingException e) {
      throw new RuntimeException("Error on the `cc` definition of the smtp connection. Error: " + e.getMessage());
    }

    try {
      this.defaultBcc = BMailAddressStatic.addressAndNamesToListInternetAddress(
        (String) this.getAttribute(SmtpConnectionAttributeEnum.BCC).getValueOrDefault(),
        (String) this.getAttribute(SmtpConnectionAttributeEnum.BCC_NAMES).getValueOrDefault()
      );
    } catch (AddressException | UnsupportedEncodingException e) {
      throw new RuntimeException("Error on the `cc` definition of the smtp connection. Error: " + e.getMessage());
    }
  }

  private Boolean getTls() {

    return (Boolean) getAttribute(SmtpConnectionAttributeEnum.TLS).getValueOrDefault();

  }

  private Boolean getDebug() {

    return (Boolean) getAttribute(SmtpConnectionAttributeEnum.DEBUG).getValueOrDefault();

  }


  private String getSmtpPassword() throws NoValueException {

    return this.getPassword();

  }

  private String getSmtpUser() throws NoValueException {

    return (String) this.getUser().getValueOrDefault();


  }

  public Integer getPort() {

    return (Integer) this.getAttribute(ConnectionAttributeEnumBase.PORT).getValueOrDefault();

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
    return null;
  }


  @Override
  public DataPath getRuntimeDataPath(DataPath dataPath, MediaType mediaType) {
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
      return true;
    } catch (MessagingException e) {
      return false;
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

    smtpConfig.setHost(this.getHost().toString());

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


  private String getDefaultFromProperty() {

    String from = (String) this.getAttribute(SmtpConnectionAttributeEnum.FROM).getValueOrDefault();
    if (from != null) {
      return from;
    }
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

  @Override
  public List<Class<? extends ConnectionAttributeEnum>> getAttributeEnums() {
    List<Class<? extends ConnectionAttributeEnum>> attributeEnums = new ArrayList<>(super.getAttributeEnums());
    attributeEnums.add(SmtpConnectionAttributeEnum.class);
    return attributeEnums;
  }

  public InternetAddress getDefaultFromInternetAddress() {
    return this.defaultFrom;
  }
}
