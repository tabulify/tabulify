package net.bytle.vertx.analytics.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SignInEvent extends AnalyticsServerEvent {


  private String oauthProviderHandle;
  private String oauthProviderGuid;

  @Override
  public AnalyticsEventType getType() {
    return AnalyticsEventType.SIGN_IN;
  }

  /**
   * The oauth provider used
   */
  public void setOAuthProviderHandle(String oAuthProviderHandle) {
    this.oauthProviderHandle = oAuthProviderHandle;
  }

  public void setOAuthProviderGuid(String oAuthProviderId) {
    this.oauthProviderGuid = oAuthProviderId;
  }

  @JsonProperty("OAuthProviderHandle")
  public String getOauthProviderHandle() {
    return oauthProviderHandle;
  }

  @JsonProperty("OAuthProviderGuid")
  public String getOauthProviderGuid() {
    return oauthProviderGuid;
  }

}
