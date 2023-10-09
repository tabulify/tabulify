package net.bytle.tower.util;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CSPHandler;
import net.bytle.tower.TowerApp;

public class CspUtil {

  /**
   * Example github CSP:
   * ```
   * default-src 'none';
   * base-uri 'self';
   * block-all-mixed-content;
   * child-src github.com/assets-cdn/worker/ gist.github.com/assets-cdn/worker/;
   * connect-src 'self' uploads.github.com objects-origin.githubusercontent.com www.githubstatus.com collector.github.com raw.githubusercontent.com api.github.com github-cloud.s3.amazonaws.com github-production-repository-file-5c1aeb.s3.amazonaws.com github-production-upload-manifest-file-7fdce7.s3.amazonaws.com github-production-user-asset-6210df.s3.amazonaws.com cdn.optimizely.com logx.optimizely.com/v1/events *.actions.githubusercontent.com productionresultssa0.blob.core.windows.net/ productionresultssa1.blob.core.windows.net/ productionresultssa2.blob.core.windows.net/ productionresultssa3.blob.core.windows.net/ productionresultssa4.blob.core.windows.net/ wss://*.actions.githubusercontent.com github-production-repository-image-32fea6.s3.amazonaws.com github-production-release-asset-2e65be.s3.amazonaws.com insights.github.com wss://alive.github.com;
   * font-src github.githubassets.com;
   * form-action 'self' github.com gist.github.com objects-origin.githubusercontent.com;
   * frame-ancestors 'self';
   * frame-src viewscreen.githubusercontent.com notebooks.githubusercontent.com;
   * img-src 'self' data: github.githubassets.com media.githubusercontent.com camo.githubusercontent.com identicons.github.com avatars.githubusercontent.com github-cloud.s3.amazonaws.com objects.githubusercontent.com objects-origin.githubusercontent.com secured-user-images.githubusercontent.com/ user-images.githubusercontent.com/ private-user-images.githubusercontent.com opengraph.githubassets.com github-production-user-asset-6210df.s3.amazonaws.com customer-stories-feed.github.com spotlights-feed.github.com *.githubusercontent.com;
   * manifest-src 'self';
   * media-src github.com user-images.githubusercontent.com/ secured-user-images.githubusercontent.com/ private-user-images.githubusercontent.com;
   * script-src github.githubassets.com;
   * style-src 'unsafe-inline' github.githubassets.com;
   * worker-src github.com/assets-cdn/worker/ gist.github.com/assets-cdn/worker/
   * ```n
   */

  /**
   * Csp Handler
   * <a href="https://vertx.io/docs/vertx-web/java/#_csp_handler">...</a>
   * <p></p>
   * For now, path matching, but nonce matching seems more
   * flexible
   * See <a href="https://datacadamia.com/web/http/csp#allow_resources_by_nonce_matching">...</a>
   */
  public static void addCspDirective(Router rootRouter, TowerApp app) {

    /**
     * The value from keycloak was:
     * `frame-src 'self'; frame-ancestors 'self'; object-src 'none';`
     */
    CSPHandler cspHandler = CSPHandler.create();

    /**
     * Add scripts
     */
    String[] scriptSrc = {
      "https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/",
      /**
       * For swagger
      */
      "https://unpkg.com/swagger-ui-dist@4.4.1/",
      "https://cdn.jsdelivr.net/npm/js-yaml@4.1.0/"
    };
    cspHandler.addDirective("default-src", String.join(" ", scriptSrc));

    /**
     * Allow inline svg
     * `Refused to load the image 'data:image/`
     */
    cspHandler.addDirective("img-src", "'self' data:;");

    /**
     * Add it for all routes of the app
     */
    String path = app.getAbsoluteLocalPathWithDomain() + "/*";
    rootRouter.route(path).handler(cspHandler);

  }

}
