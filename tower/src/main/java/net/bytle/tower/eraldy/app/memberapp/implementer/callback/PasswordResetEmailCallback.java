package net.bytle.tower.eraldy.app.memberapp.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.app.memberapp.EraldyMemberApp;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.util.AuthInternalAuthenticator;
import net.bytle.tower.util.JwtClaimsObject;
import net.bytle.vertx.HttpStatus;

/**
 * Handle the password reset
 */
public class PasswordResetEmailCallback extends FlowCallbackAbs {


  private static final String FRONT_END_UPDATE_OPERATION_PATH = "/login/password/update";

  private static PasswordResetEmailCallback passwordResetEmailCallback;


  public static PasswordResetEmailCallback getOrCreate(EraldyMemberApp eraldyMemberApp) {
    if (PasswordResetEmailCallback.passwordResetEmailCallback != null) {
      return PasswordResetEmailCallback.passwordResetEmailCallback;
    }
    PasswordResetEmailCallback.passwordResetEmailCallback = new PasswordResetEmailCallback(eraldyMemberApp);
    return PasswordResetEmailCallback.passwordResetEmailCallback;
  }

  public PasswordResetEmailCallback(EraldyMemberApp eraldyMemberApp) {
    super(eraldyMemberApp);
  }


  /**
   * @return the operation path that will handle the click of the validation link in the email
   */
  @Override
  public String getOriginOperationPath() {
    return "/login/password/reset";
  }


  /**
   * Handle the clink on the login link
   */
  public void handle(RoutingContext ctx) {

    JwtClaimsObject jwtClaimsObject;
    try {
      jwtClaimsObject = getAndValidateJwtClaims(ctx, "password reset");
    } catch (IllegalStructure e) {
      return;
    }

    String email = jwtClaimsObject.getEmail();
    String realmHandle = jwtClaimsObject.getRealmHandle();
    UserProvider.createFrom(ctx.vertx())
      .getUserByEmail(email, realmHandle)
      .onFailure(ctx::fail)
      .onSuccess(userInDb -> {
        if (userInDb == null) {
          ctx.fail(HttpStatus.INTERNAL_ERROR, new InternalException("The user (" + email + "," + realmHandle + ")  send by mail, does not exist"));
          return;
        }
        AuthInternalAuthenticator
          .createWith(ctx, userInDb)
          .redirectViaFrontEnd(FRONT_END_UPDATE_OPERATION_PATH)
          .authenticate();
      });

  }


}
