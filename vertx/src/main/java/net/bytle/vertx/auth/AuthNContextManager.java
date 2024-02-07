package net.bytle.vertx.auth;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.bytle.vertx.flow.WebFlow;

import java.util.ArrayList;
import java.util.List;

/**
 * The authN manager is a class that permits
 * to configure the final authentication behavior
 * known as {@link AuthNContext}
 */
public class AuthNContextManager {
  private final Builder builder;

  public AuthNContextManager(Builder builder) {
    this.builder = builder;
  }

  public static Builder builder() {
    return new Builder();
  }

  public AuthNContext newAuthNContext(RoutingContext ctx, WebFlow webFlow, AuthUser authUser, OAuthState oAuthState, AuthJwtClaims jwtClaims) {
      return new AuthNContext(this, webFlow, ctx, authUser, oAuthState, jwtClaims);
  }

  public String getRealmGuidSessionKey() {
    return this.builder.realmGuidSessionKey;
  }

  public List<Handler<AuthNContext>> getHandlers() {
    return builder.authContextHandlers;
  }


  public static class Builder {

    List<Handler<AuthNContext>> authContextHandlers = new ArrayList<>();
    private String realmGuidSessionKey;

    public Builder addContextHandler(Handler<AuthNContext> authContextHandler) {
      authContextHandlers.add(authContextHandler);
      return this;
    }

    /**
     * @param realmGuidSessionKey - the session key that permits to retrieve the realm guid on the session
     *                        This is used to verify that the users logged is also from the same realm
     */
    public Builder setRealmGuidSessionKey(String realmGuidSessionKey) {
      this.realmGuidSessionKey = realmGuidSessionKey;
      return this;
    }

    public AuthNContextManager build() {
      return new AuthNContextManager(this);
    }


  }
}
