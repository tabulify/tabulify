package net.bytle.vertx;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * Browser authorization for CROSS request
 */
public class BrowserCorsUtil {


  /**
   * <a href="https://vertx.io/docs/vertx-web/java/#_cors_handling">...</a>
   *
   * @param router - the rooter
   */
  public static void allowCorsForApexDomain(Router router, TowerApexDomain towerApexDomain) {

    /**
     * Cors
     * The allowed headers and method
     * than a fetch request can use in a browser
     */
    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add(HttpHeaders.CONTENT_TYPE);
    allowedHeaders.add(HttpHeaders.ACCEPT);
    /**
     * Authorization to log in with a JWT bearer
     */
    allowedHeaders.add(HttpHeaders.AUTHORIZATION);
    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);
    String routePath = towerApexDomain.getAbsoluteLocalPath() + "/*";
    String scheme = HttpsCertificateUtil.createOrGet().getHttpScheme();
    /**
     * Allow to receive credentials from everywhere
     * This will respond with the `Access-Control-Allow-Origin` sets to the origin
     * <p>
     * ie with the origin `http://app.combostrap.local:8083`, you will get
     * ```
     * Access-Control-Allow-Origin: http://app.combostrap.local:8083
     * ```
     */
    String regexAllowed = scheme + ":\\/\\/.*";
    router.route(routePath).handler(
      CorsHandler
        .create()
        // allow all subdomain
        .addRelativeOrigin(regexAllowed)
        // allow the sending of cookies
        .allowCredentials(true)
        .allowedHeaders(allowedHeaders)
        .maxAgeSeconds(60 * 60 * 24) // 1 day ?
        .allowedMethods(allowedMethods)
    );
  }
}
