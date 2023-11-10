package net.bytle.tower.eraldy.api.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.util.AuthInternalAuthenticator;
import net.bytle.vertx.HttpStatus;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.flow.FlowCallbackAbs;

/**
 * Handle the password reset
 */
public class PasswordResetEmailCallback extends FlowCallbackAbs {


  private static final String FRONT_END_UPDATE_OPERATION_PATH = "/login/password/update";

  private static PasswordResetEmailCallback passwordResetEmailCallback;


  public static PasswordResetEmailCallback getOrCreate(TowerApp towerApp) {
    if (PasswordResetEmailCallback.passwordResetEmailCallback != null) {
      return PasswordResetEmailCallback.passwordResetEmailCallback;
    }
    PasswordResetEmailCallback.passwordResetEmailCallback = new PasswordResetEmailCallback(towerApp);
    return PasswordResetEmailCallback.passwordResetEmailCallback;
  }

  public PasswordResetEmailCallback(TowerApp towerApp) {
    super(towerApp);
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

    String email = authUser.getEmail();
    String realmIdentifier = authUser.getRealmIdentifier();
    EraldyApiApp apiApp = (EraldyApiApp) this.getApp();
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
          .createWith(apiApp, ctx, userInDb)
          .redirectViaFrontEnd(FRONT_END_UPDATE_OPERATION_PATH)
          .authenticate();
      });

  }


}
