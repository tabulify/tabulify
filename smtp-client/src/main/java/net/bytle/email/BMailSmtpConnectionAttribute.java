package net.bytle.email;

import jakarta.mail.internet.InternetAddress;
import net.bytle.type.Attribute;



public enum BMailSmtpConnectionAttribute implements Attribute {


  /**
   * Attribute of a connection
   */
  AUTH("if authentication is mandatory (deprecated)", Boolean.class, DEFAULTS.AUTH),
  TLS("use secure connection", Boolean.class, DEFAULTS.TLS),
  // Default same as php
  // https://www.php.net/manual/en/mail.configuration.php
  HOST("the smtp host server", String.class, DEFAULTS.HOST),
  PORT("the smtp port of the service", Integer.class, DEFAULTS.PORT),
  PROTOCOL("The smtp protocol", String.class, DEFAULTS.SMTP_SCHEME),
  USER("The smtp user auth ", String.class, null),

  PASSWORD( "The smtp user password", String.class, null),

  FROM("The sender address", InternetAddress.class, null),
  FROM_NAME("The name of the sender", String.class, null),
  TO("The receivers addresses", null, null),
  TO_NAMES("The receivers names", null, null),
  CC("The carbon copy addresses", null, null),
  CC_NAMES("The carbon copy names", null, null),
  BCC("The blind carbon copy addresses", null, null),
  BCC_NAMES("The blind carbon copy names", null, null)
  ;



  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;

  BMailSmtpConnectionAttribute(String description, Class<?> clazz, Object def) {
    this.description = description;
    this.clazz = clazz;
    this.defaultValue = def;
  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  public <T> T getDefaultValue(Class<T> clazz) {
    return clazz.cast(this.defaultValue);
  }

  public static class DEFAULTS {



    /**
     * The default connection is a local one
     * and has therefore no auth, no tls
     */
    public static final String SMTP_SCHEME = BMailSmtpProtocol.SMTP.toString();
    public static final String HOST = "localhost";
    public static final boolean AUTH = false;
    public static final boolean TLS = false;
    public static final Integer PORT = 25;


  }

  @Override
  public String toString() {
    // The connection attribute are lowercase with JMail
    // ie smtp.host and not smtp.HOST
    return this.name().toLowerCase();
  }
}
