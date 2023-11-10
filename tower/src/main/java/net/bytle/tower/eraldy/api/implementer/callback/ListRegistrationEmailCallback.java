package net.bytle.tower.eraldy.api.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.flow.ListRegistrationFlow;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.auth.AuthUser;
import net.bytle.vertx.flow.FlowCallbackAbs;

/**
 * List registration callback
 */
public class ListRegistrationEmailCallback extends FlowCallbackAbs {


  private static ListRegistrationEmailCallback listRegistrationEmailCallback;

  public ListRegistrationEmailCallback(TowerApp eraldyMemberApp) {
    super(eraldyMemberApp);
  }


  public static ListRegistrationEmailCallback getOrCreate(TowerApp eraldyMemberApp) {
    if (listRegistrationEmailCallback != null) {
      return listRegistrationEmailCallback;
    }
    listRegistrationEmailCallback = new ListRegistrationEmailCallback(eraldyMemberApp);
    return listRegistrationEmailCallback;
  }


  /**
   * @return the operation path that is the origin
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
    } catch (IllegalStructure e) {
      return;
    }
    ListRegistrationFlow.handleStep2EmailValidationLinkClick((EraldyApiApp) this.getApp(), ctx, authUser);

  }


}
