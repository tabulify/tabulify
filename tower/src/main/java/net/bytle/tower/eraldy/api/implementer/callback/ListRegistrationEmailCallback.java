package net.bytle.tower.eraldy.api.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.tower.eraldy.api.implementer.flow.ListRegistrationFlow;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.flow.WebFlowEmailCallbackAbs;

/**
 * List registration callback
 */
public class ListRegistrationEmailCallback extends WebFlowEmailCallbackAbs {

  public ListRegistrationEmailCallback(ListRegistrationFlow listRegistrationFlow) {
    super(listRegistrationFlow);
  }

  /**
   * @return the operation path that is the start point of the flow
   */
  public String getOriginOperationPath() {
    return "/auth/register/list";
  }


  /**
   * Handle the clink on the validation link
   */
  @Override
  public void handle(RoutingContext ctx) {

    AuthUser authUser;
    try {
      authUser = getAndValidateJwtClaims(ctx, "list registration");
    } catch (IllegalStructure | TowerFailureException e) {
      return;
    }
    getWebFlow().handleStep2EmailValidationLinkClick(ctx, authUser);

  }

  @Override
  public ListRegistrationFlow getWebFlow() {
    return (ListRegistrationFlow) super.getWebFlow();
  }

}
