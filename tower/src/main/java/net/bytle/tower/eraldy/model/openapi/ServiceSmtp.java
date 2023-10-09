package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Smtp Mail Service configuration creation and/or modification
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceSmtp   {

  private String host;
  private Integer port;
  private String startTls;
  private String userName;
  private String password;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ServiceSmtp () {
  }

  /**
  * @return host The smtp server hostname
  */
  @JsonProperty("host")
  public String getHost() {
    return host;
  }

  /**
  * @param host The smtp server hostname
  */
  @SuppressWarnings("unused")
  public void setHost(String host) {
    this.host = host;
  }

  /**
  * @return port The smtp server port (generally 587 or 25)
  */
  @JsonProperty("port")
  public Integer getPort() {
    return port;
  }

  /**
  * @param port The smtp server port (generally 587 or 25)
  */
  @SuppressWarnings("unused")
  public void setPort(Integer port) {
    this.port = port;
  }

  /**
  * @return startTls SSL Secure connection (one of required, optional, disable)
  */
  @JsonProperty("startTls")
  public String getStartTls() {
    return startTls;
  }

  /**
  * @param startTls SSL Secure connection (one of required, optional, disable)
  */
  @SuppressWarnings("unused")
  public void setStartTls(String startTls) {
    this.startTls = startTls;
  }

  /**
  * @return userName Login Username
  */
  @JsonProperty("userName")
  public String getUserName() {
    return userName;
  }

  /**
  * @param userName Login Username
  */
  @SuppressWarnings("unused")
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
  * @return password Login Password
  */
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }

  /**
  * @param password Login Password
  */
  @SuppressWarnings("unused")
  public void setPassword(String password) {
    this.password = password;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceSmtp serviceSmtp = (ServiceSmtp) o;
    return Objects.equals(host, serviceSmtp.host) &&
        Objects.equals(port, serviceSmtp.port) &&
        Objects.equals(startTls, serviceSmtp.startTls) &&
        Objects.equals(userName, serviceSmtp.userName) &&
        Objects.equals(password, serviceSmtp.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port, startTls, userName, password);
  }

  @Override
  public String toString() {
    return "class ServiceSmtp {\n" +
    "}";
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
