package net.bytle.vertx.auth;

import io.vertx.ext.web.handler.impl.APIKeyHandlerImpl;

/**
 * An APIKey handler
 * <p>
 * It adds the capability to get the super token
 * to create test request
 */
public class ApiKeyAuthenticationHandler extends APIKeyHandlerImpl {


  public ApiKeyAuthenticationHandler(ApiTokenAuthenticationProvider authProvider) {
    super(authProvider);

  }

  public String getSuperToken() {
    return ((ApiTokenAuthenticationProvider) this.authProvider).getSuperToken();
  }
}
