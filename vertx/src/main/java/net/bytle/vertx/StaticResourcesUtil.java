package net.bytle.vertx;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import net.bytle.java.JavaEnvs;
import net.bytle.type.UriEnhanced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that add the handle of static resources for
 * an app and for the doc
 */
public class StaticResourcesUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(StaticResourcesUtil.class);

  /**
   * This is:
   * * the directory where the static assets are stored
   * * the requested operation path for assets
   */
  public static String ASSETS_PATH_OPERATION = "/assets";

  /**
   * The static website
   * <a href="https://vertx.io/docs/vertx-web/java/#_serving_static_resources">...</a>
   */

  public static StaticHandler getStaticHandlerForRelativeResourcePath(String relativeRootPath) {
    StaticHandler staticHandler;
    /**
     * Caching
     */
    if (JavaEnvs.IS_DEV) {
      /**
       * We set this property to prevent Vert.x caching files loaded from the classpath on disk
       * This means if you edit the static files in your IDE then the next time they are served the new ones will
       * be served without you having to restart the main()
       * This is only useful for development - do not use this in a production server
       * <p>
       * Seems to be not working at this place
       */
      System.setProperty("vertx.disableFileCaching", "true");
      staticHandler = StaticHandler
        .create(FileSystemAccess.RELATIVE, "src/main/resources/" + relativeRootPath)
        .setCachingEnabled(false);
      LOGGER.info("Static resources started with cache disabled");
    } else {
      /**
       * Relative to the working directory and classPath
       */
      staticHandler = StaticHandler
        .create(relativeRootPath);
      LOGGER.info("Static resources started with cache enabled");
    }
    return staticHandler;
  }

  /**
   * Add the static handler for resources assets for an app.
   */
  public static void addStaticHandlerAndAssetsReroutingForApp(Router rootRouter, TowerApp app) {


    String absoluteStaticResourcePath = app.getAbsoluteStaticResourcesPath();
    StaticHandler staticHandler = StaticResourcesUtil.getStaticHandlerForRelativeResourcePath("/" + HtmlUtil.WEBROOT + absoluteStaticResourcePath);
    rootRouter.get(absoluteStaticResourcePath + "/*").handler(staticHandler);

    /**
     * Reroute public static request
     * ie `member.combostrap.com/assets` to `webroot/combo/member/assets`
     * https://vertx.io/docs/vertx-web/java/#_reroute
     */
    rootRouter
      .route() // all equivalent to /*
      .handler(ctx -> {

        RoutingContextWrapper ctxWrapper = RoutingContextWrapper.createFrom(ctx);

        if (ctxWrapper.isReRouteOccurring()) {
          ctx.next();
          return;
        }

        String remoteHost = HttpRequestUtil.getRemoteHost(ctx);
        if (!remoteHost.equals(app.getPublicDomainHost())) {
          ctx.next();
          return;
        }

        HttpServerRequest httpRequest = ctx.request();
        String path = httpRequest.path();
        if (!path.startsWith(ASSETS_PATH_OPERATION)) {
          ctx.next();
          return;
        }

        /**
         * Don't reroute full qualified path
         * to avoid a recursion because that's what
         * the rerouting below does
         */
        String absoluteInternalPath = app.getAbsoluteStaticResourcesPath();
        if (path.startsWith(absoluteInternalPath)) {
          ctx.next();
          return;
        }


        /**
         * https://vertx.io/docs/vertx-web/java/#_reroute
         * Note that this method will silently discard and ignore any html fragment from the path
         */
        String newPath;
        String requestRoute = path.substring(ASSETS_PATH_OPERATION.length());
        newPath = absoluteInternalPath + requestRoute;
        String reRouteString = UriEnhanced.create().setPath(newPath)
          .setQueryString(httpRequest.query())
          .toUri()
          .toString();


        ctxWrapper.reroute(reRouteString);

      });

  }
}
