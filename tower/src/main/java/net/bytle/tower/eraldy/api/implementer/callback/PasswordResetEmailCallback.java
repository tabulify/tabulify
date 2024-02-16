package net.bytle.tower.eraldy.api.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.flow.PasswordResetFlow;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.auth.AuthJwtClaims;
import net.bytle.vertx.auth.OAuthState;
import net.bytle.vertx.flow.WebFlowEmailCallbackAbs;

/**
 * Handle the password reset callback
 */
public class PasswordResetEmailCallback extends WebFlowEmailCallbackAbs {


  private static final String FRONT_END_UPDATE_OPERATION_PATH = "/login/password/update";


  public PasswordResetEmailCallback(PasswordResetFlow webFlow) {
    super(webFlow);
  }


  /**
   * @return the operation path that will handle the click of the validation link in the email
   */
  @Override
  public String getOriginOperationPath() {
    return "/auth/login/password/reset";
  }


  /**
   * Handle the clink on the reset link
   */
  public void handle(RoutingContext ctx) {

    AuthJwtClaims jwtClaims;
    try {
      jwtClaims = getAndValidateJwtClaims(ctx, "password reset");
    } catch (IllegalStructure | TowerFailureException e) {
      return;
    }

    String email = jwtClaims.getSubjectEmail();
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

    String realmIdentifier = jwtClaims.getRealmGuid();
    EraldyApiApp apiApp = (EraldyApiApp) this.getWebFlow().getApp();
    apiApp
      .getAuthProvider()
      .getAuthUserForSessionByEmailNotNull(emailAddress, realmIdentifier)
      .onFailure(ctx::fail)
      .onSuccess(authUserForSession -> apiApp.getAuthNContextManager().newAuthNContext(ctx, webFlow, authUserForSession, OAuthState.createEmpty(), jwtClaims)
        .redirectViaHttp(apiApp.getEraldyModel().getMemberAppUri().setPath(FRONT_END_UPDATE_OPERATION_PATH))
        .authenticateSession());

  }


}
