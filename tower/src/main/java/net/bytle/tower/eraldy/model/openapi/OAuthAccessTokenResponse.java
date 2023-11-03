package net.bytle.tower.eraldy.model.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * The access token response
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthAccessTokenResponse   {


  protected String accessToken;

  protected String tokenType;

  protected String scope;

  protected String expiresIn;

  protected String refreshToken;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public OAuthAccessTokenResponse () {
  }

  /**
  * @return accessToken The access token string as issued by the authorization server (required)
  */
  @JsonProperty("access_token")
  public String getAccessToken() {
    return accessToken;
  }

  /**
  * @param accessToken The access token string as issued by the authorization server (required)
  */
  @SuppressWarnings("unused")
  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  /**
  * @return tokenType The type of token (ie how the token should be passed) (required)
  */
  @JsonProperty("token_type")
  public String getTokenType() {
    return tokenType;
  }

  /**
  * @param tokenType The type of token (ie how the token should be passed) (required)
  */
  @SuppressWarnings("unused")
  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  /**
  * @return scope The scopes separated by a comma (Optional)
  */
  @JsonProperty("scope")
  public String getScope() {
    return scope;
  }

  /**
  * @param scope The scopes separated by a comma (Optional)
  */
  @SuppressWarnings("unused")
  public void setScope(String scope) {
    this.scope = scope;
  }

  /**
  * @return expiresIn The duration of time the access token is granted for, if the access token expires (Optional)
  */
  @JsonProperty("expires_in")
  public String getExpiresIn() {
    return expiresIn;
  }

  /**
  * @param expiresIn The duration of time the access token is granted for, if the access token expires (Optional)
  */
  @SuppressWarnings("unused")
  public void setExpiresIn(String expiresIn) {
    this.expiresIn = expiresIn;
  }

  /**
  * @return refreshToken A refresh token to obtain another access token (if the access token expired), (Optional)
  */
  @JsonProperty("refresh_token")
  public String getRefreshToken() {
    return refreshToken;
  }

  /**
  * @param refreshToken A refresh token to obtain another access token (if the access token expired), (Optional)
  */
  @SuppressWarnings("unused")
  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OAuthAccessTokenResponse oauthAccessTokenResponse = (OAuthAccessTokenResponse) o;
    return Objects.equals(accessToken, oauthAccessTokenResponse.accessToken) &&
        Objects.equals(tokenType, oauthAccessTokenResponse.tokenType) &&
        Objects.equals(scope, oauthAccessTokenResponse.scope) &&
        Objects.equals(expiresIn, oauthAccessTokenResponse.expiresIn) &&
        Objects.equals(refreshToken, oauthAccessTokenResponse.refreshToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessToken, tokenType, scope, expiresIn, refreshToken);
  }

  @Override
  public String toString() {
    return "class OAuthAccessTokenResponse {\n" +

    "    accessToken: " + toIndentedString(accessToken) + "\n" +

    "    tokenType: " + toIndentedString(tokenType) + "\n" +
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
