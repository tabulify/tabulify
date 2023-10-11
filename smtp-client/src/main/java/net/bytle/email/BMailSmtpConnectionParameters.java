package net.bytle.email;

/**
 * An object to hold and pass smtp connection
 * configuration parameters around from vertx to log, the BMail
 */
public class BMailSmtpConnectionParameters {


    private Integer defaultSmtpPort;
    private String hostname;
    private BMailStartTls defaultSmtpStartTlsOption;
    private String userName;
    private String password;
    private BMailInternetAddress sender;



    /**
     * @param dkimSelector - the dkim selector used to select the public key (test in test and combo in production)
     * @return the config for chaining
     */


    /**
     * @param localSmtpPort - the port of the local smtp service
     * @return the config for chaining
     * It's used primarily to set the port of the wiser smtp service
     * during test
     */
    public BMailSmtpConnectionParameters setDefaultSmtpPort(Integer localSmtpPort) {
    this.defaultSmtpPort = localSmtpPort;
    return this;
  }

    public BMailSmtpConnectionParameters setHostname(String hostname) {
    this.hostname = hostname;
    return this;
  }

    public BMailSmtpConnectionParameters setDefaultStartTls(BMailStartTls startTLSOptions) {
    this.defaultSmtpStartTlsOption = startTLSOptions;
    return this;
  }

    public BMailSmtpConnectionParameters setUserName(String username) {
    this.userName = username;
    return this;
  }

    public BMailSmtpConnectionParameters setDefaultSmtpPassword(String password) {
    this.password = password;
    return this;
  }

    public BMailSmtpConnectionParameters setSender(BMailInternetAddress sender) {
    this.sender = sender;
    return this;
  }

  public String getHost() {
    return this.hostname;
  }

  public String getPassword() {
    return this.password;
  }

  public String getUserName() {
    return this.userName;
  }

  public Integer getPort() {
    return this.defaultSmtpPort == null ? BMailSmtpConnectionAttribute.PORT.getDefaultValue(Integer.class): this.defaultSmtpPort;
  }

  public BMailStartTls getStartTlsOption() {
    return this.defaultSmtpStartTlsOption;
  }

  public BMailInternetAddress getSender() {
    return this.sender;
  }

}
