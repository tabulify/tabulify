package net.bytle.tower.eraldy.app.memberapp.implementer.callback;

import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.tower.eraldy.app.memberapp.EraldyMemberApp;
import net.bytle.tower.eraldy.app.memberapp.implementer.flow.ListRegistrationFlow;
import net.bytle.tower.util.JwtClaimsObject;

/**
 * List registration callback
 */
public class ListRegistrationEmailCallback extends FlowCallbackAbs {


  private static ListRegistrationEmailCallback listRegistrationEmailCallback;

  public ListRegistrationEmailCallback(EraldyMemberApp eraldyMemberApp) {
    super(eraldyMemberApp);
  }


  public static ListRegistrationEmailCallback getOrCreate(EraldyMemberApp eraldyMemberApp) {
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
    return "/register/list";
  }


  /**
   * Handle the clink on the validation link
   */
  @Override
  public void handle(RoutingContext ctx) {

    JwtClaimsObject jwtClaimsObject;
    try {
      jwtClaimsObject = getAndValidateJwtClaims(ctx, "list registration");
    } catch (IllegalStructure e) {
      return;
    }
    ListRegistrationFlow.handleStep2EmailValidationLinkClick(ctx, jwtClaimsObject);

  }


}
