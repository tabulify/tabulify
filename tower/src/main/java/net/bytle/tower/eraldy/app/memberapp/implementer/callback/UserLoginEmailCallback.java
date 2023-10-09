package net.bytle.tower.eraldy.app.memberapp.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.app.memberapp.EraldyMemberApp;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.util.AuthInternalAuthenticator;
import net.bytle.tower.util.JwtClaimsObject;

/**
 * The letter (in HTML format)
 * that is sent by email to log a user
 * by clicking on a login link
 */
public class UserLoginEmailCallback extends FlowCallbackAbs {


  private static UserLoginEmailCallback userRegistration;


  public static UserLoginEmailCallback getOrCreate(EraldyMemberApp eraldyMemberApp) {
    if (UserLoginEmailCallback.userRegistration != null) {
      return UserLoginEmailCallback.userRegistration;
    }
    UserLoginEmailCallback.userRegistration = new UserLoginEmailCallback(eraldyMemberApp);
    return UserLoginEmailCallback.userRegistration;
  }

  public UserLoginEmailCallback(EraldyMemberApp eraldyMemberApp) {
    super(eraldyMemberApp);
  }


  /**
   * @return the origin operation path
   */
  @Override
  public String getOriginOperationPath() {
    return "/login/email";
  }


  /**
   * Handle the clink on the login link
   */
  public void handle(RoutingContext ctx) {

    JwtClaimsObject jwtClaimsObject;
    try {
      jwtClaimsObject = getAndValidateJwtClaims(ctx,"login");
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
          ctx.fail(500, new InternalException("The user send by mail, does not exist"));
          return;
        }
        AuthInternalAuthenticator
          .createWith(ctx, userInDb)
          .redirectViaHttp()
          .authenticate();
      });

  }

}
