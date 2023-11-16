package net.bytle.tower.eraldy.api.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.flow.PasswordResetFlow;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.vertx.HttpStatusEnum;
import net.bytle.vertx.auth.AuthContext;
import net.bytle.vertx.auth.AuthState;
import net.bytle.vertx.auth.AuthUser;
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
          ctx.fail(HttpStatusEnum.INTERNAL_ERROR_500.getStatusCode(), new InternalException("The user (" + email + "," + realmIdentifier + ")  send by mail, does not exist"));
          return;
        }
        new AuthContext(this.getWebFlow().getApp(), ctx, UsersUtil.toAuthUser(userInDb), AuthState.createEmpty())
          .redirectViaFrontEnd(FRONT_END_UPDATE_OPERATION_PATH)
          .authenticateSession();
      });

  }


}
