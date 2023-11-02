package net.bytle.vertx;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.XFrameHandler;

/**
 * Security Util for HTML pages app
 */
public class BrowserSecurityUtil {

  /**
   * Add Security directives for HTML pages app
   * <p></p>
   * It should be added, after session and body handler
   */
  public static void addSecurityDirectives(Router rootRouter, TowerApp app) {

    String path = app.getPathMount() + "/*";

    /**
     * Don't render in a frame, iframe, embed or object
     * https://vertx.io/docs/vertx-web/java/#_xframe_handler
     */
    rootRouter.route(path).handler(XFrameHandler.create(XFrameHandler.DENY));

    /**
     * Csp
     */
    CspUtil.addCspDirective(rootRouter, app);

    /**
     * CSRF
     */
    VertxCsrf.addCsrfCookieToken(rootRouter, app);

    /**
     * X-XSS Protection
     */
    rootRouter.route(path).handler(ctx -> {
      /**
       * This header configures the Cross-site scripting (XSS) filter in your browser. Using the default behaviour, the browser will prevent rendering of the page when a XSS attack is detected
       * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-XSS-Protection
       */
      ctx.response().putHeader("X-XSS-Protection", "1; mode=block");
      ctx.next();
    });

    /**
     * Search engine
     */
    if (!app.isSocial()) {
      rootRouter.route(path).handler(ctx -> {
        /**
         * Default value prevents Internet Explorer and Google Chrome from MIME-sniffing a response away from the declared content-type
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Content-Type-Options
         */
        ctx.response().putHeader("X-Content-Type-Options", "nosniff");
        /**
         * Prevent pages from appearing in search engines Learn more
         * https://developers.google.com/search/docs/advanced/robots/robots_meta_tag
         */
        ctx.response().putHeader("X-Robots-Tag", "none");
        ctx.next();
      });
    }


  }

}
