package net.bytle.vertx;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;

public class TowerAppRequestBuilder {

  private final String path;
  private final WebClient webClient;
  private final TowerApp towerApp;
  private boolean withForwardProxyHostHeader = false;
  private boolean withSuperApiToken;
  private boolean withPublicDomain = false;
  private HttpMethod method = HttpMethod.GET;
  private String bearerToken;

  public TowerAppRequestBuilder(TowerApp towerApp, WebClient webClient, String path) {
    this.towerApp = towerApp;
    this.webClient = webClient;
    this.path = path;
  }

  public HttpRequest<Buffer> build() {
    String requestUri = towerApp.getPublicRequestUriForOperationPath(this.path).toUrl().toString();
    HttpRequest<Buffer> httpRequest;
    switch (this.method.name()) {
      case "POST":
        httpRequest = webClient.postAbs(requestUri);
        break;
      case "GET":
        httpRequest = webClient.getAbs(requestUri);
        break;
      default:
        throw new RuntimeException("The method " + this.method.name() + " is unknown");
    }

    if (this.withForwardProxyHostHeader) {
      /**
       * Simulate behind a {@link net.bytle.tower.util.HttpForwardProxy forward proxy}
       */
      httpRequest.putHeader(HttpHeaders.X_FORWARDED_HOST, towerApp.getPublicDomainHost());
    }
    if (this.withSuperApiToken) {
      String httpAuthorization = new UsernamePasswordCredentials("user", getSuperToken()).toHttpAuthorization();
      httpRequest.putHeader(HttpHeaderNames.AUTHORIZATION.toString(), httpAuthorization);
      httpRequest.putHeader(HttpHeaders.X_API_KEY, getSuperToken());
    }
    if (this.bearerToken != null) {
      // https://vertx.io/docs/vertx-auth-jwt/java/#_authnauthz_with_jwt
      httpRequest.putHeader(HttpHeaderNames.AUTHORIZATION.toString(), "bearer " + this.bearerToken);
    }
    if (this.withPublicDomain) {
      httpRequest.putHeader(HttpHeaders.HOST, towerApp.getPublicDomainHost());
    }
    return httpRequest;
  }

  private String getSuperToken() {
    return towerApp.getApexDomain().getHttpServer().getApiKeyAuthenticator().getSuperToken();
  }

  public TowerAppRequestBuilder withForwardProxyHeader() {
    this.withForwardProxyHostHeader = true;
    return this;
  }

  public TowerAppRequestBuilder withSuperApiToken() {
    this.withSuperApiToken = true;
    return this;
  }

  public TowerAppRequestBuilder withPublicDomainHost() {
    this.withPublicDomain = true;
    return this;
  }

  public TowerAppRequestBuilder asPost() {
    this.method = HttpMethod.POST;
    return this;
  }

  public TowerAppRequestBuilder withBearerToken(String accessToken) {
    this.bearerToken = accessToken;
    return this;
  }

}
