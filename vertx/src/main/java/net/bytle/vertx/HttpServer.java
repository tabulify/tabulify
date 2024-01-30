package net.bytle.vertx;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.SessionStore;
import net.bytle.exception.IllegalConfiguration;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.exception.NullValueException;
import net.bytle.java.JavaEnvs;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an HTTP server:
 * with standard handlers
 */
public class HttpServer implements AutoCloseable {

  private final HttpServer.builder builder;
  private Router router;

  private APIKeyHandler apiKeyAuthenticator;
  private JWTAuthHandler bearerAuthenticationHandler;
  private BasicAuthHandler basicAuthenticator;
  private PersistentLocalSessionStore persistentSessionStore;
  private BodyHandler bodyHandler;


  public HttpServer(builder builder) {
    this.builder = builder;
  }


  public static builder createFromServer(Server server) {

    return new HttpServer.builder(server);

  }


  public Router getRouter() {
    return this.router;
  }


  /**
   * This port is the port that is all external communication
   * (in email, in oauth callback, ...)
   */

  public Future<io.vertx.core.http.HttpServer> buildVertxHttpServer() {
    HttpServerOptions options = new HttpServerOptions()
      .setLogActivity(false)
      .setHost(this.builder.server.getListeningHost())
      .setPort(this.builder.server.getListeningPort());
    /**
     * https://vertx.io/docs/apidocs/io/vertx/core/net/PemKeyCertOptions.html
     */
    if (this.getServer().getSsl()) {
      if (!JavaEnvs.IS_DEV) {
        return Future.failedFuture(new InternalException("In non-dev environment, the management of certificate is not done. Ssl should off and handled by the proxy"));
      }
      options
        .setPemKeyCertOptions(
          new PemKeyCertOptions().addKeyPath(Server.DEV_KEY_PEM).addCertPath(Server.DEV_CERT_PEM)
        )
        .setSsl(true);
    }
    io.vertx.core.http.HttpServer httpServer = this.builder.server.getVertx().createHttpServer(options);
    /**
     * Future.join and not Future.all to finish all futures
     * With Future.all if there is an error, the future still
     * running produce an error when we close Vertx and it adds noise
     */
    return Future.join(this.futuresToExecuteOnBuild)
      .recover(err -> Future.failedFuture(new InternalException("A future failed while building the http server", err)))
      .compose(asyncResult -> {
        /**
         * Health Check
         * At the end because the services can register
         * during the build
         * (The argument is passed by reference, it may then also work at the beginning?)
         */
        if (this.builder.enableHealthCheck) {
          HttpServerHealth.addHandler(this);
        }
        httpServer.requestHandler(router); // https://vertx.io/docs/vertx-core/java/#_handling_requests
        return Future.succeededFuture(httpServer);
      });
  }

  public int getPublicPort() {
    return this.builder.server.getPublicPort();
  }


  /**
   * @return the scheme always secure
   */
  public String getHttpScheme() {
    if (isHttpsEnabled()) return "https";
    return "http";
  }

  /**
   * @return if https is enabled on the system
   * See the https.md documentation for more info.
   */
  public boolean isHttpsEnabled() {

    return this.builder.server.getSsl();
  }

  public Server getServer() {
    return this.builder.server;
  }


  /**
   * @return the JWT bearer authentication handler
   * An utility class, JWT should be enabled on the server
   */
  @SuppressWarnings("unused")
  public JWTAuthHandler getBearerAuthenticationHandler() {
    if (bearerAuthenticationHandler == null) {
      bearerAuthenticationHandler = JWTAuthHandler.create(this.getServer().getJwtAuth().getProvider());
    }
    return this.bearerAuthenticationHandler;
  }

  /**
   * @return the API key handler
   * An utility class, API key should be enabled on the server
   */
  public APIKeyHandler getApiKeyAuthHandler() {
    if (this.apiKeyAuthenticator == null) {
      throw new InternalException("Api Key Handler was not initialized");
    }
    return this.apiKeyAuthenticator;
  }

  /**
   * @return a basic auth handler
   */
  @SuppressWarnings("unused")
  public BasicAuthHandler getBasicAuthHandler() {
    if (this.basicAuthenticator == null) {
      try {
        this.basicAuthenticator = BasicAuthHandler.create(this.getServer().getApiKeyAuthProvider());
      } catch (NullValueException e) {
        throw new InternalException("Api Key is not enabled on the server", e);
      }
    }
    return this.basicAuthenticator;
  }


  @Override
  public void close() throws Exception {
    if (this.persistentSessionStore != null) {
      this.persistentSessionStore.close();
    }
    this.getServer().close();
  }

  public SessionStore getPersistentSessionStore() {
    if (this.persistentSessionStore == null) {
      throw new InternalException("Session store was not enabled");
    }
    return this.persistentSessionStore;
  }

  public BodyHandler getBodyHandler() throws NotFoundException {
    if (this.bodyHandler == null) {
      throw new NotFoundException();
    }
    return this.bodyHandler;
  }

  /**
   * Future to execute on build
   */
  List<Future<?>> futuresToExecuteOnBuild = new ArrayList<>();

  public void addFutureToExecuteOnBuild(Future<?> future) {
    futuresToExecuteOnBuild.add(future);
  }

  public static class builder {

    private static final String BODY_HANDLER_CONF = "body.handler.upload-dir";
    private boolean addBodyHandler = true;
    private boolean addWebLog = true;
    private boolean isBehindProxy = true;
    private boolean enableFailureHandler = true;
    private boolean enableMetrics = true;
    private boolean fakeErrorHandler = false;

    final Server server;
    private boolean enablePersistentSessionStore = false;
    /**
     * Enable the Health Check end point
     */
    private boolean enableHealthCheck = true;


    public builder(Server server) {
      this.server = server;
    }


    /**
     * Logging Web Request
     */
    public HttpServer.builder addWebLog() {
      this.addWebLog = true;
      return this;
    }

    /**
     * Forward proxy is disabled by default
     */
    public HttpServer.builder setBehindProxy() {
      this.isBehindProxy = true;
      return this;
    }

    public HttpServer.builder enableFailureHandler() {
      this.enableFailureHandler = true;
      return this;
    }

    public HttpServer.builder addMetrics() {
      this.enableMetrics = true;
      return this;
    }

    public HttpServer.builder addFakeErrorHandler() {
      this.fakeErrorHandler = true;
      return this;
    }

    /**
     * A handler which gathers the entire request body and sets it on the {@link RoutingContext}
     * You can't request the body from the request after-wards
     * You need to get if from the context object
     * <p>
     * BodyHandler is required to process POST requests for instance
     */
    public HttpServer.builder addBodyHandler() {
      this.addBodyHandler = true;
      return this;
    }

    private Router buildRouter(HttpServer httpServer) throws IllegalConfiguration {

      Vertx vertx = this.server.getVertx();
      Router router = Router.router(vertx);
      if (this.addBodyHandler) {
        /**
         * It works also with OpenApi
         */
        int bodyLimit5mb = 1024 * 1024 * 5;
        BodyHandler bodyHandler = BodyHandler
          .create()
          .setHandleFileUploads(true)
          .setBodyLimit(bodyLimit5mb);
        String uploadDir = this.server.getConfigAccessor().getString(BODY_HANDLER_CONF);
        if (uploadDir == null) {
          if (JavaEnvs.IS_DEV) {
            uploadDir = "build/vertx-uploaded-files";
          } else {
            uploadDir = "temp/vertx-uploaded-files";
          }
        }
        bodyHandler.setUploadsDirectory(uploadDir);
        httpServer.bodyHandler = bodyHandler;
        router.route().handler(bodyHandler);
      }
      if (this.addWebLog) {
        router.route().handler(new WebLogger(LoggerFormat.DEFAULT));
      }
      if (this.isBehindProxy) {
        HttpForwardProxy.addAllowForwardProxy(router);
      }
      if (this.enableFailureHandler) {

        /**
         * Failure Handler / Route match failures
         * https://vertx.io/docs/vertx-web/java/#_route_match_failures
         */
        TowerFailureHttpHandler errorHandlerXXX = TowerFailureHttpHandler.createOrGet(server);
        router.errorHandler(TowerFailureTypeEnum.INTERNAL_ERROR_500.getStatusCode(), errorHandlerXXX);

        /**
         * Handle the failures. ie
         * ```
         * ctx.fail(400, error)
         * ```
         */
        router.route().failureHandler(errorHandlerXXX);

      }
      if (this.enableMetrics) {
        MainLauncher.prometheus.mountOnRouter(router, VertxPrometheusMetrics.DEFAULT_METRICS_PATH);
      }
      if (this.fakeErrorHandler) {
        /**
         * Produce an error, not only for dev, also for production to live test (mail, ...)
         */
        router.get(ErrorFakeHandler.URI_PATH).handler(new ErrorFakeHandler());
      }
      return router;
    }

    public HttpServer build() throws IllegalConfiguration {
      HttpServer httpServer = new HttpServer(this);
      httpServer.router = this.buildRouter(httpServer);

      if (this.enablePersistentSessionStore) {
        httpServer.persistentSessionStore = PersistentLocalSessionStore.create(httpServer);
      }
      try {
        httpServer.apiKeyAuthenticator = APIKeyHandler.create(this.server.getApiKeyAuthProvider());
      } catch (NullValueException e) {
        // not configured
      }
      return httpServer;
    }


    public HttpServer.builder enablePersistentSessionStore() {
      this.enablePersistentSessionStore = true;
      return this;
    }

    public HttpServer.builder addHealthCheck() {
      this.enableHealthCheck = true;
      return this;
    }
  }
}
