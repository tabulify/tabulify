package net.bytle.vertx;

import io.vertx.core.Future;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.HSTSHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.template.api.Template;
import net.bytle.type.UriEnhanced;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * This class represents an app that is served by
 * the Tower Vertx Web Server.
 * <p>
 * It's the entry point for app related operations such as:
 * * {@link #getTemplate(String) getting a template}
 * * {@link #getRequestUriForStaticResource(UriEnhanced, String) getting the URL of a static resources}
 * * building the Route with {@link #openApiMount(RouterBuilder)} and the {@link #openApiBindSecurityScheme(RouterBuilder, ConfigAccessor) security}
 * <p>
 * This class gives three kinds of path:
 * * the relative path (path that does not start with a / - used in resources path)
 * * the absolute path (path that starts with a /)
 * * the public information for the URL path: the URL path used when an external request is made via the {@link #getPublicDomainHost()}  public host}
 * * the spec resource path: the path in the java main resources to locate the spec file
 * * the static resource path: the path of the static files in the java main resources
 * * the template resource path: the path of the html template file in the java main resources
 */
public abstract class TowerApp {


  public static final String OPENAPI_YAML_PATH = "/openapi.yaml";
  public static final String SUPERUSER_TOKEN_CONF = "superuser.token";

  /**
   * The first directory in the resource main directory
   * for the location of the spec file
   */
  private static final String OPEN_API_RESOURCES_PREFIX = "openapi-spec-file";
  private final TowerApexDomain apexDomain;
  private final ConfigAccessor configAccessor;
  private ProxyUtil proxy;
  private WebClient webClient;


  public TowerApp(TowerApexDomain towerApexDomain) {

    this.apexDomain = towerApexDomain;
    this.configAccessor = apexDomain.getHttpServer().getServer().getConfigAccessor();

  }

  public String getRelativeSpecFileResourcesPath() {
    return OPEN_API_RESOURCES_PREFIX + getAbsoluteDomainName() + "/" + getAppName() + OPENAPI_YAML_PATH;
  }


  private String getAbsoluteDomainName() {
    return "/" + getApexDomain().getPathName();
  }


  /**
   * The internal name of the component
   * used in path as identifiant (other than java)
   * <p>
   */
  public abstract String getAppName();


  /**
   * @param routingContext - the vertx routing context from a request
   * @param operationPath  - the operation path
   * @return a public or local uri with the path that depends on the HTTP request URL
   */
  public UriEnhanced getPublicOrLocalRequestUri(RoutingContext routingContext, String operationPath) {

    UriEnhanced remoteBaseUri = HttpRequestUtil.geRemoteBaseUri(routingContext);
    return getPublicOrLocalRequestUri(remoteBaseUri, operationPath);

  }

  /**
   * @param remoteRequestUri - the chosen URI (public or localhost)
   * @param operationPath    - the operation path
   * @return the URI
   */
  public UriEnhanced getPublicOrLocalRequestUri(UriEnhanced remoteRequestUri, String operationPath) {

    String hostWithPort = remoteRequestUri.getHostWithPort();
    String publicDomainHostWithPort = getPublicDomainHost();
    if (hostWithPort.equals(publicDomainHostWithPort)) {
      return remoteRequestUri.setPath(getPathMount() + operationPath);
    }
    if (!hostWithPort.startsWith(HttpRequestUtil.LOCALHOST)) {
      throw new IllegalArgumentException("For the app (" + this + "), the host (" + hostWithPort + ") should be " + publicDomainHostWithPort + " or starts with " + HttpRequestUtil.LOCALHOST + ". We couldn't create a request uri for the operation (" + operationPath + ")");
    }
    return remoteRequestUri.setPath(getPathMount() + operationPath);

  }


  /**
   * @param operationPath - the operation path
   * @return the public uri for an operation
   */
  public UriEnhanced getPublicRequestUriForOperationPath(String operationPath) {

    return this.createPublicUriHostOnly().setPath(getPathMount() + operationPath);

  }

  /**
   * This is used before any redirection.
   *
   * @param ctx - the request context
   * @return the request uri in a public format
   */
  public UriEnhanced getPublicRequestUriFromRoutingContext(RoutingContext ctx) {
    UriEnhanced requestUri;
    try {
      requestUri = UriEnhanced.createFromString(ctx.request().absoluteURI());
    } catch (IllegalStructure e) {
      throw new RuntimeException(e);
    }
    String path = requestUri.getPath();
    String absoluteLocalPathWithDomain = this.getPathMount();
    if (path.startsWith(absoluteLocalPathWithDomain)) {
      path = path.substring(absoluteLocalPathWithDomain.length());
      requestUri.setPath(path);
    }
    return requestUri;
  }


  public abstract TowerApp openApiMount(RouterBuilder builder);

  /**
   * To add security handlers for the openApi Security handler
   * Configuring `AuthenticationHandler`s defined in the OpenAPI document
   * <a href="https://vertx.io/docs/vertx-web-openapi/java/#_configuring_authenticationhandlers_defined_in_the_openapi_document">...</a>
   */
  public abstract TowerApp openApiBindSecurityScheme(RouterBuilder builder, ConfigAccessor configAccessor);


  /**
   * @return the location of the static resources
   * We mount it as a path operation
   * It permits to add handler
   * on the whole app or domain
   */
  public String getAbsoluteStaticResourcesPath() {
    return this.getPathMount() + StaticResourcesUtil.ASSETS_PATH_OPERATION;
  }

  /**
   * @param templateName - the file name with the html extension
   * @return the template
   */
  public Template getTemplate(String templateName) {
    String templateResourcesPath = getApexDomain().getPathName() + "/" + this.getAppName() + "/" + templateName;
    return TemplateEngine.getLocalHtmlEngine(this.apexDomain.getHttpServer().getServer().getVertx())
      .compile(templateResourcesPath);
  }

  public TowerAppRequestBuilder getRequestBuilder(String path) {
    if (webClient == null) {
      Server server = this.getApexDomain().getHttpServer().getServer();
      webClient = HttpClientBuilder.builder(server.getVertx())
        .withServerProperties(server)
        .buildWebClient();
    }
    return new TowerAppRequestBuilder(this, webClient, path);
  }

  /**
   * @return the public host (the value is the same than {@link HttpRequest#host()}
   * (ie it contains the port part)
   */
  public String getPublicDomainHost() {
    return this.getPublicSubdomainName() + "." + getApexDomain().getApexName();
  }

  /**
   * The name of the subdomain
   * Ie foo in foo@example.com
   */
  protected abstract String getPublicSubdomainName();


  @SuppressWarnings("unused")
  private UriEnhanced getRequestUriForStaticResource(UriEnhanced remoteRequestUri, String name) {

    String absoluteStaticPath = getAbsoluteStaticResourcesPath();
    return remoteRequestUri.setPath(absoluteStaticPath + "/" + name);

  }

  public Future<Void> mount() {
    return mountOnRouter();
  }

  private Future<Void> mountOnRouter() {


    Router rootRouter = apexDomain.getHttpServer().getRouter();

    /**
     * Browser Specific
     */
    if (getIsHtmlApp()) {

      /**
       * Add Security
       */
      BrowserSecurityUtil.addSecurityDirectives(rootRouter, this);

      /**
       * Set the handler to serve static resources
       * (and the rerouting)
       */
      StaticResourcesUtil.addStaticHandlerAndAssetsReroutingForApp(rootRouter, this);

    }

    /**
     * Reroute
     * Before refactoring, it should be:
     * * at the top (almost first)
     * * just after {@link StaticResourcesUtil#addStaticHandlerAndAssetsReroutingForApp(Router, TowerApp) static resources rerouting}
     */
    this.reRouteOrRedirect(rootRouter);

    /**
     * Mandatory HTTPS: HTTP Strict Transport Security (HSTS)
     * https://vertx.io/docs/vertx-web/java/#_hsts_handler
     * <p>
     * The Strict-Transport-Security HTTP header tells browsers to always use HTTPS.
     */
    if (getApexDomain().getHttpServer().isHttpsEnabled()) {
      String pathMount = this.getPathMount();
      Route route;
      if (!pathMount.equals("")) {
        route = rootRouter.route(pathMount);
      } else {
        route = rootRouter.route();
      }
      /**
       * With the value `max-age=31536000; includeSubDomains`
       * Once a browser sees this header, it will only visit the site over HTTPS
       * for the time specified (1 year) at max-age,
       * including the subdomains.
       */
      route.handler(HSTSHandler.create(31536000, true));
    }


    /**
     * Add specific handlers
     * Note that as they may need a session, they are placed after the browser session
     */
    this.addSpecificAppHandlers(rootRouter);


    /**
     * Load the open api
     * <p>
     * This is blocking because we need to call
     * the proxy at the end of the router so that the proxy requests have a lower priority
     * We could make it non-blocking but the code would be replicated in the if block with openapi
     * and without any openapi
     * It will then not be really self-explaining.
     * <p>
     * It needs to be in a executeBlocking block because of the CompletableFuture
     * Otherwise, it seems that the `CompletableFuture.get` is called
     * quickly and that the mountOpenApi code goes into the wild (ie is not executed)
     */
    return this.apexDomain.getHttpServer().getServer().getVertx().executeBlocking(() -> {
      CompletableFuture<String> openApiMounter = new CompletableFuture<>();
      if (this.hasOpenApiSpec()) {
        /**
         * Mount OpenApi
         */
        OpenApiUtil.config(this)
          .build()
          .mountOpenApi(rootRouter)
          .onFailure(
            // Something went wrong during router builder initialization
            // "Unable to parse the openApi specification. Error: "
            FailureStatic::failFutureWithTrace
          )
          .onSuccess(asyncResult -> openApiMounter.complete("done"));
      } else {
        openApiMounter.complete("done");
      }

      try {
        /**
         * The proxy handling is a `catch-all` handler
         * and should then be at the end to get a lower priority
         * than any other routes.
         */
        openApiMounter.get(60, TimeUnit.SECONDS);
        this.addProxyHandlerForUnknownResourceOfHtmlApp(rootRouter);
        return null;
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        throw new RuntimeException("Unable to load the open api spec", e);
      }

    });


  }

  /**
   * Add handler to the rooter for this app
   * This is used for custom handlers such as OAuthExternal
   * or Authorization handlers
   *
   * @param router - the router
   * @return for fluency
   */
  protected abstract TowerApp addSpecificAppHandlers(Router router);

  /**
   * This handler will pass through all
   * unknown resources in the HTML page
   * created by the dev server
   * such as reload, image,
   * <p>
   * It should be at the end of the rout configuration
   * to get a low priority.
   */
  public void addProxyHandlerForUnknownResourceOfHtmlApp(Router router) {
    if (getIsHtmlApp()) {
      ProxyUtil.addProxyHandler(router, this);
    }
  }

  /**
   * @return does this app has an openapi file
   */
  public abstract boolean hasOpenApiSpec();

  /**
   * We reroute public request to internal path
   * ie `api.combostrap.com/` to `combo/public/`
   * ie `api.combostrap.com/_private` to `combo/private/`
   * ie `member.combostrap.com/` to `combo/member/`
   * <a href="https://vertx.io/docs/vertx-web/java/#_reroute">...</a>
   * <p>
   * We redirect public request with an internal path to a full public request
   * ie `member.combostrap.local:8083/combo/member/oauth/protected` to `member.combostrap.local:8083/oauth/protected`
   */
  public TowerApp reRouteOrRedirect(Router rootRouter) {

    String pathMount = this.getPathMount();
    if (pathMount == null) {
      throw new InternalException("The public absolute path mount should not be null for the app (" + this + ")");
    }

    /**
     * Reroute a URL without path to the default operation
     * if this is not mounted to root
     */
    if (
      !pathMount.equals("")
        && !pathMount.equals("/")
    ) {
      rootRouter
        .get(pathMount)
        .handler(ctx -> {
          String publicDefaultOperationPath = this.getDefaultOperationPath();
          if (publicDefaultOperationPath == null || publicDefaultOperationPath.equals("")) {
            ctx.next();
            return;
          }
          String uri = getPublicOrLocalRequestUri(ctx, publicDefaultOperationPath).toUri().toString();
          ctx.redirect(uri);
        });
    }

    return this;

  }

  /**
   * @return the path operation where to redirect when the URL has only the domain. The call is then redirected this operation path
   */

  public abstract String getDefaultOperationPath();


  /**
   * @return the path where the app should be mounted
   * <p>
   * Example:
   * * for the private api, the value us `/_private` meaning that the private path is `api.combostrap.com/_private`
   * * for the ip api, the value is the empty string `` meaning that the base path is `api.combostrap.com/`
   */
  public abstract String getPathMount();

  /**
   * @return true if this App returns HTML page (ie not an API app)
   * This app is used primarily with the browser.
   * <p>
   * This is used to add specific Browser HTML securities such as
   * CSRF and X-Frame headers
   * See {@link BrowserSecurityUtil}
   */
  public abstract boolean getIsHtmlApp();


  /**
   * @return a proxy instance for this app request
   */
  public ProxyUtil getProxy() {
    if (this.proxy != null) {
      return this.proxy;
    }
    Boolean useFiddler = configAccessor.getBoolean("forward.proxy.fiddler", false);
    this.proxy = ProxyUtil
      .config(this)
      .setProxyThroughFiddler(useFiddler)
      .build();
    return this.proxy;
  }

  /**
   * @return true if the app can be indexed by search engine
   */
  public abstract boolean isSocial();

  /**
   * The public URI is:
   * * the public top level domain
   * * the public port (80 by default but 8083 on dev)
   * <p>
   * The public URI is saved hard core in external place such as OAuthExternal
   * It is:
   * * in dev `app.combostrap.local:8083`
   * * in prod `app.combostrap.com`
   */
  public UriEnhanced createPublicUriHostOnly() {

    /**
     * We create a URI each time as it may be accessed multiple time in one request,
     * and we don't want to mix query parameters
     */
    String scheme = apexDomain.getHttpServer().getHttpScheme();
    try {
      return UriEnhanced.create()
        .setScheme(scheme)
        .setHost(this.getPublicDomainHost());
    } catch (IllegalStructure e) {
      throw new IllegalArgumentException("Unable to set the public Uri for the app (" + this + ")", e);
    }

  }


  /**
   * @return the name used in the configuration file
   */
  public String getAppConfName() {
    return (this.getApexDomain().getPathName() + "." + this.getAppName()).toLowerCase();
  }


  public TowerApexDomain getApexDomain() {
    return this.apexDomain;
  }

  @Override
  public String toString() {
    return getApexDomain().getPathName() + "." + getAppName();
  }

  /**
   * Utility method that returns if the request is for this app
   *
   * @param routingContext - the routing context
   * @return if the request is for this app
   */
  public boolean isAppRequest(RoutingContext routingContext) {
    return RoutingContextWrapper.createFrom(routingContext).getOriginalRequestAsUri().getHostWithPort().equals(getPublicDomainHost());
  }

}
