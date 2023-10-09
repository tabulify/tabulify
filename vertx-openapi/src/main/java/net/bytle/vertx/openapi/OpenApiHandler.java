package net.bytle.vertx.openapi;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.Collections;
import java.util.List;


/**
 * Handler
 */
public final class OpenApiHandler implements Handler<RoutingContext> {

  private OpenAPI openAPI = null;

  private final Router router;
  private String title;
  private String version;
  private boolean build = false;
  private String url;



  public OpenApiHandler(Router router) {
    this.router = router;
  }
  public OpenApiHandler(Router router, OpenAPI openApi) {
    this.openAPI = openApi;
    this.router = router;
  }

  public static OpenApiHandler createFromRouter(Router router) {
    return new OpenApiHandler(router);
  }

  public OpenApiHandler setTitle(String title) {
    this.title = title;
    return this;
  }

  public OpenApiHandler setVersion(String version) {
    this.version = version;
    return this;
  }

  public OpenApiHandler setURL(String url) {
    this.url = url;
    return this;
  }

  @Override
  public void handle(RoutingContext routingContext) {

    if (!build){
      build=true;
      if (this.openAPI==null) {
        this.openAPI = new OpenAPI();
      }
      Info info = openAPI.getInfo();
      if (info==null) {
        info = new Info();
        openAPI.setInfo(info);
      }
      if (title!=null) {
        info.setTitle(title);
      }
      if (version!=null) {
        info.setVersion(version);
      }
      List<Server> server = openAPI.getServers();
      if (server == null || server.size()==0){
        openAPI.servers(Collections.singletonList(new Server()));
        openAPI.getServers().forEach(s->setURL(url));
      }
      OpenApiSpecBuilder.createFrom(this.router).build(openAPI);
    }


    String path = routingContext.normalisedPath();
    String type = path.substring(path.lastIndexOf(".")+1);
    switch (type){
      case "yml":
      case "yaml":
        routingContext.response()
          .putHeader("Content-Type", "text/plain")
          .end(Yaml.pretty(this.openAPI));
      default:
        String json = Json.pretty(this.openAPI);
        routingContext.response()
          .putHeader("Content-Type", "application/json")
          .end(json);
        break;
    }

  }
}
