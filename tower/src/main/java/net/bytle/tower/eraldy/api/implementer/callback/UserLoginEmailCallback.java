package net.bytle.tower.eraldy.api.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.auth.AuthJwtClaims;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.auth.OAuthState;
import net.bytle.vertx.flow.WebFlow;
import net.bytle.vertx.flow.WebFlowEmailCallbackAbs;

/**
 * The end point for the login link
 * sends via email
 */
public class UserLoginEmailCallback extends WebFlowEmailCallbackAbs {


  public UserLoginEmailCallback(WebFlow webFlow) {
    super(webFlow);
  }


  /**
   * @return the origin operation path
   */
  @Override
  public String getOriginOperationPath() {
    return "/auth/login/email";
  }


  /**
   * Handle the clink on the login link
   */
  public void handle(RoutingContext ctx) {

    AuthJwtClaims jwtClaims;
    try {
      jwtClaims = getAndValidateJwtClaims(ctx, "login");
    } catch (IllegalStructure | TowerFailureException e) {
      return;
    }

    AuthUser authUser = jwtClaims.toAuthUser();
    String email = authUser.getSubjectEmail();
    EmailAddress emailAddress;
    try {
      emailAddress = EmailAddress.of(email);
    } catch (EmailCastException e) {
      TowerFailureException
        .builder()
        .setMessage("The AUTH subject email (" + email + ") is not valid.")
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .buildWithContextFailingTerminal(ctx);
      return;
    }
    String realmGuid = authUser.getRealmGuid();
    EraldyApiApp apiApp = (EraldyApiApp) this.getWebFlow().getApp();
    apiApp
      .getAuthProvider()
      .getAuthUserForSessionByEmailNotNull(emailAddress, realmGuid)
      .onFailure(ctx::fail)
      .onSuccess(authUserForSession -> apiApp.getAuthNContextManager()
        .newAuthNContext(ctx, webFlow, authUserForSession, OAuthState.createEmpty(), jwtClaims)
        .redirectViaHttpToAuthRedirectUri()
        .authenticateSession());

  }

}
