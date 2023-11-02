package net.bytle.tower.eraldy.app.memberapp.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.tower.eraldy.app.memberapp.EraldyMemberApp;
import net.bytle.tower.eraldy.app.memberapp.implementer.flow.UserRegistrationFlow;
import net.bytle.vertx.JwtClaimsObject;

/**
 * The letter (in HTML format)
 * that is sent by email to validate a user registration
 * by clicking on the validation link
 */
public class UserRegisterEmailCallback extends FlowCallbackAbs {


  private static UserRegisterEmailCallback userRegistration;

  public UserRegisterEmailCallback(EraldyMemberApp eraldyMemberApp) {
    super(eraldyMemberApp);
  }


  public static UserRegisterEmailCallback getOrCreate(EraldyMemberApp eraldyMemberApp) {
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
    return "/register/user";
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
    UserRegistrationFlow.handleStep2ClickOnEmailValidationLink(ctx, jwtClaimsObject);


  }




}
