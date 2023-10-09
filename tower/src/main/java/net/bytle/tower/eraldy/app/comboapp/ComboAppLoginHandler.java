package net.bytle.tower.eraldy.app.comboapp;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.app.memberapp.EraldyMemberApp;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.util.ContextFailureData;
import net.bytle.tower.util.HttpStatus;
import net.bytle.tower.util.RoutingContextWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle the login of the combo application
 * and redirect to the login page if not
 */
public class ComboAppLoginHandler implements Handler<RoutingContext> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(ComboAppLoginHandler.class);
  private final ComboAppApp app;

  public ComboAppLoginHandler(ComboAppApp comboAppApp) {
    this.app = comboAppApp;
  }

  @Override
  public void handle(RoutingContext ctx) {

    /**
     * Same as {@link io.vertx.ext.web.handler.RedirectAuthHandler}
     */
    OrganizationUser user;
    try {
      user = RoutingContextWrapper.createFrom(ctx)
        .getSignedInUserAsOrganizationUser();
    } catch (NotFoundException e) {
      String publicRequestUri = app.getPublicRequestUriFromRoutingContext(ctx).toUri().toString();
      String loginUrl = EraldyMemberApp
        .get()
        .getLoginUriForEraldyRealm(publicRequestUri)
        .toUrl()
        .toString();
      ctx.redirect(loginUrl);
      return;
    }

    /**
     * Is Organizational user
     */
    if (user.getOrganization() == null) {
      String publicRequestUri = app.getPublicRequestUriFromRoutingContext(ctx).toUri().toString();
      String logoutUrl = EraldyMemberApp
        .get()
        .getLogoutUriForEraldyRealm(publicRequestUri)
        .toUrl()
        .toString();
      ContextFailureData.create()
        .setStatusCode(HttpStatus.NOT_AUTHORIZED)
        .setName("Not authorized")
        .setDescription("The user (" + user.getEmail() + ") is not authorized to login to the combo app. You can login with another user by clicking on this <a href=\"" + logoutUrl + "\">link</a>.")
        .failContextAsHtml(ctx);
      return;
    }

    /**
     * next
     */
    ctx.next();


  }
}
