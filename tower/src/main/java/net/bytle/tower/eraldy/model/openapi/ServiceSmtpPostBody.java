package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Smtp Mail Service creation and/or modification  * For the service, for an upsert, you may give the service Guid or the service Uri * For the impersonated user, you may give an email or its guid
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceSmtpPostBody   {


  protected String serviceGuid;

  protected String serviceUri;

  protected String realmGuid;

  protected String realmHandle;

  protected String impersonatedUserEmail;

  protected String impersonatedUserGuid;

  protected String smtpHost;

  protected Integer smtpPort;

  protected String smtpStartTls;

  protected String smtpUserName;

  protected String smtpPassword;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public ServiceSmtpPostBody () {
  }

  /**
  * @return serviceGuid The service global id, if you want to update the identifier
  */
  @JsonProperty("serviceGuid")
  public String getServiceGuid() {
    return serviceGuid;
  }

  /**
  * @param serviceGuid The service global id, if you want to update the identifier
  */
  @SuppressWarnings("unused")
  public void setServiceGuid(String serviceGuid) {
    this.serviceGuid = serviceGuid;
  }

  /**
  * @return serviceUri The service uri
  */
  @JsonProperty("serviceUri")
  public String getServiceUri() {
    return serviceUri;
  }

  /**
  * @param serviceUri The service uri
  */
  @SuppressWarnings("unused")
  public void setServiceUri(String serviceUri) {
    this.serviceUri = serviceUri;
  }

  /**
  * @return realmGuid The realm guid
  */
  @JsonProperty("realmGuid")
  public String getRealmGuid() {
    return realmGuid;
  }

  /**
  * @param realmGuid The realm guid
  */
  @SuppressWarnings("unused")
  public void setRealmGuid(String realmGuid) {
    this.realmGuid = realmGuid;
  }

  /**
  * @return realmHandle The realm handle
  */
  @JsonProperty("realmHandle")
  public String getRealmHandle() {
    return realmHandle;
  }

  /**
  * @param realmHandle The realm handle
  */
  @SuppressWarnings("unused")
  public void setRealmHandle(String realmHandle) {
    this.realmHandle = realmHandle;
  }

  /**
  * @return impersonatedUserEmail The user email for cold email, Null for system/app service
  */
  @JsonProperty("impersonatedUserEmail")
  public String getImpersonatedUserEmail() {
    return impersonatedUserEmail;
  }

  /**
  * @param impersonatedUserEmail The user email for cold email, Null for system/app service
  */
  @SuppressWarnings("unused")
  public void setImpersonatedUserEmail(String impersonatedUserEmail) {
    this.impersonatedUserEmail = impersonatedUserEmail;
  }

  /**
  * @return impersonatedUserGuid The user guid for cold email, Null for system/app service
  */
  @JsonProperty("impersonatedUserGuid")
  public String getImpersonatedUserGuid() {
    return impersonatedUserGuid;
  }

  /**
  * @param impersonatedUserGuid The user guid for cold email, Null for system/app service
  */
  @SuppressWarnings("unused")
  public void setImpersonatedUserGuid(String impersonatedUserGuid) {
    this.impersonatedUserGuid = impersonatedUserGuid;
  }

  /**
  * @return smtpHost The smtp server hostname
  */
  @JsonProperty("smtpHost")
  public String getSmtpHost() {
    return smtpHost;
  }

  /**
  * @param smtpHost The smtp server hostname
  */
  @SuppressWarnings("unused")
  public void setSmtpHost(String smtpHost) {
    this.smtpHost = smtpHost;
  }

  /**
  * @return smtpPort The smtp server port
  */
  @JsonProperty("smtpPort")
  public Integer getSmtpPort() {
    return smtpPort;
  }

  /**
  * @param smtpPort The smtp server port
  */
  @SuppressWarnings("unused")
  public void setSmtpPort(Integer smtpPort) {
    this.smtpPort = smtpPort;
  }

  /**
  * @return smtpStartTls SSL Secure connection (one of required, optional, disable)
  */
  @JsonProperty("smtpStartTls")
  public String getSmtpStartTls() {
    return smtpStartTls;
  }

  /**
  * @param smtpStartTls SSL Secure connection (one of required, optional, disable)
  */
  @SuppressWarnings("unused")
  public void setSmtpStartTls(String smtpStartTls) {
    this.smtpStartTls = smtpStartTls;
  }

  /**
  * @return smtpUserName Login Username
  */
  @JsonProperty("smtpUserName")
  public String getSmtpUserName() {
    return smtpUserName;
  }

  /**
  * @param smtpUserName Login Username
  */
  @SuppressWarnings("unused")
  public void setSmtpUserName(String smtpUserName) {
    this.smtpUserName = smtpUserName;
  }

  /**
  * @return smtpPassword Login Password
  */
  @JsonProperty("smtpPassword")
  public String getSmtpPassword() {
    return smtpPassword;
  }

  /**
  * @param smtpPassword Login Password
  */
  @SuppressWarnings("unused")
  public void setSmtpPassword(String smtpPassword) {
    this.smtpPassword = smtpPassword;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceSmtpPostBody serviceSmtpPostBody = (ServiceSmtpPostBody) o;
    return Objects.equals(serviceGuid, serviceSmtpPostBody.serviceGuid) &&
        Objects.equals(serviceUri, serviceSmtpPostBody.serviceUri) &&
        Objects.equals(realmGuid, serviceSmtpPostBody.realmGuid) &&
        Objects.equals(realmHandle, serviceSmtpPostBody.realmHandle) &&
        Objects.equals(impersonatedUserEmail, serviceSmtpPostBody.impersonatedUserEmail) &&
        Objects.equals(impersonatedUserGuid, serviceSmtpPostBody.impersonatedUserGuid) &&
        Objects.equals(smtpHost, serviceSmtpPostBody.smtpHost) &&
        Objects.equals(smtpPort, serviceSmtpPostBody.smtpPort) &&
        Objects.equals(smtpStartTls, serviceSmtpPostBody.smtpStartTls) &&
        Objects.equals(smtpUserName, serviceSmtpPostBody.smtpUserName) &&
        Objects.equals(smtpPassword, serviceSmtpPostBody.smtpPassword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceGuid, serviceUri, realmGuid, realmHandle, impersonatedUserEmail, impersonatedUserGuid, smtpHost, smtpPort, smtpStartTls, smtpUserName, smtpPassword);
  }

  @Override
  public String toString() {
    return "class ServiceSmtpPostBody {\n" +
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
