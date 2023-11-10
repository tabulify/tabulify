package net.bytle.tower.eraldy.api.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.flow.PasswordResetFlow;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.vertx.HttpStatus;
import net.bytle.vertx.auth.AuthInternalAuthenticator;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.flow.WebFlowCallbackAbs;

/**
 * Handle the password reset callback
 */
public class PasswordResetEmailCallback extends WebFlowCallbackAbs {


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

    AuthUser authUser;
    try {
      authUser = getAndValidateJwtClaims(ctx, "password reset");
    } catch (IllegalStructure e) {
      return;
    }

    String email = authUser.getSubjectEmail();
    String realmIdentifier = authUser.getRealmIdentifier();
    EraldyApiApp apiApp = (EraldyApiApp) this.getWebFlow().getApp();
    apiApp
      .getUserProvider()
      .getUserByEmail(email, realmIdentifier)
      .onFailure(ctx::fail)
      .onSuccess(userInDb -> {
        if (userInDb == null) {
          ctx.fail(HttpStatus.INTERNAL_ERROR.httpStatusCode(), new InternalException("The user (" + email + "," + realmIdentifier + ")  send by mail, does not exist"));
          return;
        }
        AuthInternalAuthenticator
          .createWith(apiApp, ctx, UsersUtil.toAuthUserClaims(userInDb))
          .redirectViaFrontEnd(FRONT_END_UPDATE_OPERATION_PATH)
          .authenticate();
      });

  }


}
