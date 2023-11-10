package net.bytle.tower.eraldy.api.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.flow.UserRegistrationFlow;
import net.bytle.vertx.JwtClaimsObject;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.flow.FlowCallbackAbs;

/**
 * The letter (in HTML format)
 * that is sent by email to validate a user registration
 * by clicking on the validation link
 */
public class UserRegisterEmailCallback extends FlowCallbackAbs {


  private static UserRegisterEmailCallback userRegistration;

  public UserRegisterEmailCallback(TowerApp eraldyMemberApp) {
    super(eraldyMemberApp);
  }


  public static UserRegisterEmailCallback getOrCreate(TowerApp eraldyMemberApp) {
    if (userRegistration != null) {
      return userRegistration;
    }
    userRegistration = new UserRegisterEmailCallback(eraldyMemberApp);
    return userRegistration;
  }


  /**
   * @return the origin operation path
   */
  public String getOriginOperationPath() {
    return "/auth/register/user";
  }


  /**
   * Handle the clink on the validation link
   */
  @Override
  public void handle(RoutingContext ctx) {

    JwtClaimsObject jwtClaimsObject;
    try {
      jwtClaimsObject = getAndValidateJwtClaims(ctx, "registration");
    } catch (IllegalStructure e) {
      return;
    }
    UserRegistrationFlow.handleStep2ClickOnEmailValidationLink((EraldyApiApp) this.getApp(), ctx, jwtClaimsObject);


  }




}
