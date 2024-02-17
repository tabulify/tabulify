package net.bytle.vertx;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * We proxy the `get` requests
 * In other word, we mount the external node web server into the vertx web server
 * <p>
 * It's used to be able to develop a Javascript/React application with the reload goodies
 * and to be still on the same domain.
 * <p>
 * Note: The node application should be started on the same local domain for the reload to work
 * <p>
 * This is the same as `io.vertx:vertx-http-proxy`,
 * but because we allow to proxy on API, the proxy returns the body {@link #proxyRequest(RoutingContext)}
 * <p>
 * Adapted from <a href="https://github.com/vert-x3/vertx-examples/blob/4.x/core-examples/src/main/java/io/vertx/example/core/http/proxy/Proxy.java">...</a>
 * <p>
 * There is also a low-level HTTP proxy here:
 * <a href="https://github.com/eclipse-vertx/vert.x/blob/master/src/test/java/io/vertx/test/proxy/HttpProxy.java">...</a>
 * Here also an example with an API gateway:
 * <a href="https://github.com/sczyh30/vertx-blueprint-microservice/blob/master/api-gateway/src/main/java/io/vertx/blueprint/microservice/gateway/APIGatewayVerticle.java#L155">...</a>
 */
public class ForwardProxy {

  private static final Logger LOGGER = LoggerFactory.getLogger(ForwardProxy.class);

  /**
   * if no answer si received after 5 secs,
   * a timeout failure is triggered
   */
  private static final long REQUEST_TIMEOUT_SEC = 5;
  private final HttpClient httpClient;
  private final TowerApp app;
  private final Integer targetPort;
  private final String targetHost;
  private final ProxyOptions fiddlerProxy;

  public ForwardProxy(TowerApp towerApp, boolean useFiddlerProxy) {

    this.app = towerApp;


    /**
     * The port where the dev server (HTML/Javascript) is running locally
     */
    String key = this.app.getAppConfName() + ".forward.proxy.port";
    this.targetPort = this.app.getHttpServer().getServer().getConfigAccessor().getInteger(key);
    if (targetPort == null) {
      throw new RuntimeException("The port was not found in the conf " + key);
    }

    /**
     * The host
     */
    String targetHostWithoutPort = this.app.getPublicDomainHost();
    int portSeparatorLocation = targetHostWithoutPort.indexOf(":");
    if (portSeparatorLocation != -1) {
      targetHostWithoutPort = targetHostWithoutPort.substring(0, portSeparatorLocation);
    }
    this.targetHost = targetHostWithoutPort;

    /**
     * Fiddler Proxy
     * It can also be set at the request level See {@link #proxyRequest(RoutingContext, boolean)}
     */
    this.fiddlerProxy = new ProxyOptions();
    fiddlerProxy.setHost("127.0.0.1");
    fiddlerProxy.setPort(8888);

    /**
     * Http client
     */
    this.httpClient = createHttpClient(useFiddlerProxy);


  }

  private HttpClient createHttpClient(Boolean useFiddlerProxy) {

    Server server = this.app.getHttpServer().getServer();
    HttpClientBuilder towerHttpClient = HttpClientBuilder.builder(this.app.getHttpServer().getServer().getVertx())
      .withServerProperties(server)
    .setDefaultPort(targetPort)
      .setDefaultHost(targetHost);
    if (useFiddlerProxy) {
      towerHttpClient.setProxyOptions(fiddlerProxy);
    }
    /**
     * Target Host and port
     */
    return towerHttpClient.buildHttpClient();

  }

  /**
   * Proxy all other get from the dev server
   * This handler should be last, after the openapi router.
   */
  public static void addProxyHandler(Router rootRouter, TowerApp towerApp) {


    String absoluteInternalPath = towerApp.getPathMount();
    String route = absoluteInternalPath + "/*";

    ForwardProxy proxy = towerApp.getProxy();
    rootRouter.route(route).method(HttpMethod.GET).handler(ctx ->
      proxy
        .proxyRequest(ctx)
        .onSuccess(buffer -> ctx.response().send(buffer))
    );

  }


  public static config config(TowerApp towerApp) {
    return new config(towerApp);
  }

  /**
   * @param ctx - the context
   * @return a future to be able to integrate it with the api
   */
  public Future<Buffer> proxyRequest(RoutingContext ctx) {

    return proxyRequest(ctx, false);

  }

  /**
   * @param ctx        the routing context
   * @param useFiddler sends the request through Fiddler to debug
   * @return the body result
   */
  public Future<Buffer> proxyRequest(RoutingContext ctx, boolean useFiddler) {

    /**
     * The proxy code
     */
    HttpServerRequest serverRequest = ctx.request();

    /**
     * Request URI
     */
    String absoluteInternalPath = this.app.getPathMount();
    String requestUri = serverRequest.uri();
    if (requestUri.startsWith(absoluteInternalPath)) {
      requestUri = requestUri.substring(absoluteInternalPath.length());
    }

    String hostWithPort = targetHost + ":" + targetPort;
    LOGGER.debug("Proxying request: " + hostWithPort + requestUri);
    HttpServerResponse serverResponse = serverRequest.response();

    /**
     * Headers: We remove the original host to not get: `Invalid Host header`
     * <p>
     * We could modify and add the {@link HttpHeaders.X_FORWARDED_HOST}
     * but it works without
     */
    MultiMap headers = serverRequest.headers();
    headers.remove(HttpHeaders.HOST);
    if (headers.get(HttpHeaders.ACCEPT) == null) {
      // If not set, the dev server may return a 404
      headers.add(HttpHeaders.ACCEPT, HttpHeaders.ACCEPT_STANDARD_BROWSER_VALUE);
    }

    /**
     * Retrieve the body
     */
    RequestBody body = ctx.body();
    Buffer bodyBuffer;
    if (body.available()) {
      bodyBuffer = body.buffer();
      if (bodyBuffer == null) {
        bodyBuffer = Buffer.buffer();
      }
    } else {
      /**
       * In <a href="https://github.com/vert-x3/vertx-examples/blob/4.x/core-examples/src/main/java/io/vertx/example/core/http/proxy/Proxy.java">...</a>
       * They make a pause on the server request and send it.
       * Not really sure what it means
       * We don't dot it here because there is a body handler
       */
      throw new RuntimeException("Body was not processed, the proxy needs a body handler before on the router.");
    }

    /**
     * Request
     */
    RequestOptions clientRequestOptions = new RequestOptions()
      .setMethod(serverRequest.method())
      .setURI(requestUri)
      .setHeaders(headers)
      // host and port mandatory to avoid connection was closed failure
      // ERROR io.vertx.core.http.impl.HttpClientRequestImpl - Connection was closed
      .setHost(targetHost)
      .setPort(targetPort)
      .setTimeout(REQUEST_TIMEOUT_SEC * 1000);

    if (useFiddler) {
      clientRequestOptions.setProxyOptions(this.fiddlerProxy);
    }

    /**
     * Create the request
     */
    Buffer finalBodyBuffer = bodyBuffer;
    return this.httpClient
      .request(clientRequestOptions)
      .compose(
        clientRequest -> clientRequest
          .send(finalBodyBuffer)
          .compose(
            clientResponse -> {
              LOGGER.debug("Proxying response: " + clientResponse.statusCode());
              serverResponse.setStatusCode(clientResponse.statusCode());
              serverResponse.headers().setAll(clientResponse.headers());
              return clientResponse.body();
            },
            errClientResponse -> {
              String msg = "Back end failure: " + errClientResponse.getMessage();
              LOGGER.error(msg);
              serverResponse.setStatusCode(500);
              return Future.succeededFuture(Buffer.buffer(msg));
            }),
        errClientRequest -> {
          String msg = "Could not connect to " + hostWithPort + ". Error: " + errClientRequest.getMessage();
          LOGGER.error(msg);
          serverResponse.setStatusCode(500);
          return Future.succeededFuture(Buffer.buffer(msg));
        });
  }


  public static class config {
    private final TowerApp towerApp;
    private boolean useFiddler = false;

    public config(TowerApp towerApp) {
      this.towerApp = towerApp;
    }

    /**
     * To see and debug the HTTP request, we can
     * use the fiddler proxy
     * This parameters set it on and off
     *
     * @param bool - use the fiddler proxy
     */
    public config setProxyThroughFiddler(boolean bool) {
      this.useFiddler = bool;
      return this;
    }

    public ForwardProxy build() {
      return new ForwardProxy(towerApp, useFiddler);
    }

  }
}
