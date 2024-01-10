package net.bytle.tower.eraldy.api.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.IllegalStructure;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.auth.AuthContext;
import net.bytle.vertx.auth.AuthState;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.flow.WebFlow;
import net.bytle.vertx.flow.WebFlowEmailCallbackAbs;

/**
 * The letter (in HTML format)
 * that is sent by email to log a user
 * by clicking on a login link
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

    AuthUser authUser;
    try {
      authUser = getAndValidateJwtClaims(ctx, "login");
    } catch (IllegalStructure | TowerFailureException e) {
      return;
    }
    String email = authUser.getSubjectEmail();
    BMailInternetAddress bMailInternetAddress;
    try {
      bMailInternetAddress = BMailInternetAddress.of(email);
    } catch (AddressException e) {
      TowerFailureException
        .builder()
        .setMessage("The AUTH subject email (" + email + ") is not valid.")
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .buildWithContextFailingTerminal(ctx);
      return;
    }
    String realmHandle = authUser.getRealmIdentifier();
    EraldyApiApp apiApp = (EraldyApiApp) this.getWebFlow().getApp();
    apiApp
      .getAuthProvider()
      .getAuthUserForSessionByEmailNotNull(bMailInternetAddress, realmHandle)
      .onFailure(ctx::fail)
      .onSuccess(authUserForSession -> new AuthContext(apiApp, ctx, authUserForSession, AuthState.createEmpty())
        .redirectViaHttpWithAuthRedirectUriAsUri()
        .authenticateSession());

  }

}
