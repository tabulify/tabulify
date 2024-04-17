package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.auth.AuthJwtClaims;
import net.bytle.vertx.auth.OAuthState;
import net.bytle.vertx.flow.FlowType;
import net.bytle.vertx.flow.WebFlow;

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
  public FlowType getFlowType() {
    return FlowType.PASSWORD_LOGIN;
  }

  public Future<Void> login(String realmIdentifier, String email, String password, RoutingContext routingContext) {

    /**
     * TODO: We should get the clientId
     * This way we would have the realm identifier and the app identifier
     * should be the clientId or we need the redirect uri to detect the API client id
     */
    EmailAddress emailAddress;
    try {
      emailAddress = EmailAddress.of(email);
    } catch (EmailCastException e) {
      return Future.failedFuture(TowerFailureException
        .builder()
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .setMessage("The email (" + email + ") is not valid")
        .build()
      );
    }

    return this.apiApp
      .getRealmProvider()
      .getRealmFromIdentifier(realmIdentifier)
      .onFailure(err -> FailureStatic.failRoutingContextWithTrace(err, routingContext))
      .compose(realm -> apiApp.getAuthProvider()
        .getAuthUserForSessionByPasswordNotNull(emailAddress, password, realm)
        .onFailure(err -> FailureStatic.failRoutingContextWithTrace(err, routingContext))
        .compose(authUserForSession -> {
          this.apiApp.getAuthNContextManager()
            .newAuthNContext(routingContext, this, authUserForSession, OAuthState.createEmpty(), AuthJwtClaims.createEmptyClaims())
            .redirectViaClient()
            .authenticateSession();
          return Future.succeededFuture();
        }));
  }
}
