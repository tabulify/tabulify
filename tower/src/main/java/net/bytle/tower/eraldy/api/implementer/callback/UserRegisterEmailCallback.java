package net.bytle.tower.eraldy.api.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.tower.eraldy.api.implementer.flow.UserRegistrationFlow;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.flow.WebFlowEmailCallbackAbs;

/**
 * The letter (in HTML format)
 * that is sent by email to validate a user registration
 * by clicking on the validation link
 */
public class UserRegisterEmailCallback extends WebFlowEmailCallbackAbs {



  public UserRegisterEmailCallback(UserRegistrationFlow userRegistrationFlow) {
    super(userRegistrationFlow);
  }


  /**
   * @return the origin operation path that needs this callback
   */
  public String getOriginOperationPath() {
    return "/auth/register/user";
  }


  /**
   * Handle the clink on the validation link
   */
  @Override
  public void handle(RoutingContext ctx) {

    AuthUser authUser;
    try {
      authUser = getAndValidateJwtClaims(ctx, "registration");
    } catch (IllegalStructure | TowerFailureException e) {
      return;
    }
    this.getWebFlow().handleStep2ClickOnEmailValidationLink(ctx, authUser);


  }

  @Override
  public UserRegistrationFlow getWebFlow() {
    return (UserRegistrationFlow) super.getWebFlow();
  }
}
