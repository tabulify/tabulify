package net.bytle.tower.eraldy.api.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.vertx.auth.AuthSessionAuthenticator;
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
      authUser = getAndValidateJwtClaims(ctx,"login");
    } catch (IllegalStructure e) {
      return;
    }

    String email = authUser.getSubjectEmail();
    String realmHandle = authUser.getRealmIdentifier();
    EraldyApiApp apiApp = (EraldyApiApp) this.getWebFlow();
    apiApp
      .getUserProvider()
      .getUserByEmail(email, realmHandle)
      .onFailure(ctx::fail)
      .onSuccess(userInDb -> {
        if (userInDb == null) {
          ctx.fail(500, new InternalException("The user send by mail, does not exist"));
          return;
        }
        new AuthSessionAuthenticator(ctx, UsersUtil.toAuthUserClaims(userInDb), AuthState.createEmpty())
          .redirectViaHttp()
          .authenticateSession();
      });

  }

}
