package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.auth.AuthContext;
import net.bytle.vertx.auth.AuthJwtClaims;
import net.bytle.vertx.auth.OAuthState;
import net.bytle.vertx.flow.WebFlow;
import net.bytle.vertx.flow.WebFlowType;

public class PasswordLoginFlow implements WebFlow {
  private final EraldyApiApp apiApp;

  public PasswordLoginFlow(EraldyApiApp apiApp) {
    this.apiApp = apiApp;
  }

  @Override
  public TowerApp getApp() {
    return apiApp;
  }

  @Override
  public WebFlowType getFlowType() {
    return WebFlowType.PASSWORD_LOGIN;
  }

  public Future<Void> login(String realmIdentifier, String handle, String password, RoutingContext routingContext) {
    AuthJwtClaims jwtClaims = AuthJwtClaims.createEmptyClaims();
    jwtClaims.setRealmGuid(realmIdentifier);
    /**
     * TODO: We should get the clientId
     * This way we would have the realm identifier and the app identifier
     * should be the clientId or we need the redirect uri to detect the API client id
     */
    return this.apiApp
      .getRealmProvider()
      .getRealmFromIdentifier(realmIdentifier)
      .onFailure(err -> FailureStatic.failRoutingContextWithTrace(err, routingContext))
      .compose(realm -> apiApp.getAuthProvider()
        .getAuthUserForSessionByPasswordNotNull(handle, password, realm)
        .onFailure(err -> FailureStatic.failRoutingContextWithTrace(err, routingContext))
        .compose(authUserForSession -> {
          new AuthContext(this, routingContext, authUserForSession, OAuthState.createEmpty(), jwtClaims)
            .redirectViaClient()
            .authenticateSession();
          return Future.succeededFuture();
        }));
  }
}
