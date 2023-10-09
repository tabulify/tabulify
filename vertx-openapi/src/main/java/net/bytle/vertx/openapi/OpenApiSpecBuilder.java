package net.bytle.vertx.openapi;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NameNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * This class will {@link #build(OpenAPI) build} a {@link OpenAPI} object from
 * the {@link Route} of a {@link Router}
 *
 */
public final class OpenApiSpecBuilder {

  private static final Logger log = LoggerFactory.getLogger(OpenApiSpecBuilder.class);
  private final Router router;


  private OpenApiSpecBuilder(Router router) {
    this.router = router;
  }

  public void build(OpenAPI openAPI) {
    log.info("Generating Spec for vertx routes.");
    /**
     * Route Path by OpenApi {@link PathItem}
     * https://swagger.io/specification/#path-item-object
     */
    for (Route route : router.getRoutes()) {
      String path = route.getPath();
      if (path == null) {
        /**
         * When running as verticle, some route
         * are created with a null path
         */
        continue;
      }

      RouteWrapper routeWrapper = new RouteWrapper(route);
      if (routeWrapper.isNotOpenApiRoute()) {

        routeWrapper.createOpenApiOperationFromRouteMethod();

        routeWrapper.setOperationsAttributesFromHandlersAnnotations();

        /**
         * Add all path api to open api for this route
         */
        for (PathItem pathApi : routeWrapper.getPathApis()) {
          openAPI.path(routeWrapper.getPath(), pathApi);
        }
      }

    }

  }


  static public OpenApiSpecBuilder createFrom(Router router) {

    return new OpenApiSpecBuilder(router);
  }


  private List<Parameter> extractOperationParametersFromPath(String fullPath) {
    String[] split = fullPath.split("\\/");
    return Arrays.stream(split)
      .filter(x -> x.startsWith(":")).map(x -> {
        Parameter param = new Parameter();
        param.name(x.substring(1));
        return param;
      })
      .collect(Collectors.toList());
  }


  /**
   * The link between a route and its path items
   */
  private class RouteWrapper {


    private final Route route;
    Set<Class<?>> handlerClasses = new HashSet<>();

    /**
     * Map between http method and path item
     */
    private Map<HttpMethod, PathItem> httpMethodsPathItemMap = new HashMap<>();

    public RouteWrapper(Route route) {
      this.route = route;
      builtHandlers();
      buildHttpMethodPathItemMap();
    }

    public Route getRoute() {
      return this.route;
    }


    /**
     * The path of the route
     *
     * @return
     */
    public String getPath() {
      return this.route.getPath();
    }

    /**
     * The handlers for this route
     *
     * @return
     */
    public Set<Class<?>> getHandlerClasses() {
      return handlerClasses;
    }

    private void builtHandlers() {
      List<Handler<RoutingContext>> handlers;
      try {
        /**
         * Vertx 3.8.4
         */
        Field stateField = route.getClass().getDeclaredField("state");
        stateField.setAccessible(true);
        Object routeState = stateField.get(route);
        Field contextHandlersField = routeState.getClass().getDeclaredField("contextHandlers");
        contextHandlersField.setAccessible(true);
        handlers = (List<Handler<RoutingContext>>) contextHandlersField.get(routeState);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        /**
         * Vertx 3.5.0
         */
        try {
          Field contextHandlers = route.getClass().getDeclaredField("contextHandlers");
          contextHandlers.setAccessible(true);
          handlers = (List<Handler<RoutingContext>>) contextHandlers.get(route);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
          throw new RuntimeException(ex);
        }
      }
      for (Handler<RoutingContext> handler : handlers) {
        Class<?> delegate;
        try {
          /**
           * When this is a functional method (lambda).
           * Because the lambda is compiled, I don't see a way to retrieve the functional method.
           */
          delegate = handler.getClass().getDeclaredField("arg$1").getType();
        } catch (NoSuchFieldException e) {
          /**
           * When this is a functional class
           */
          delegate = handler.getClass();
        }
        handlerClasses.add(delegate);
      }

    }

    public Set<HttpMethod> getHttpMethods() {

      return httpMethodsPathItemMap.keySet();
    }

    private void buildHttpMethodPathItemMap() {

      Set<HttpMethod> httpMethods;
      try {
        /**
         * Vertx 3.8.4
         */
        Field stateField = route.getClass().getDeclaredField("state");
        stateField.setAccessible(true);
        Object routeState = stateField.get(route);
        Field methodsField = routeState.getClass().getDeclaredField("methods");
        methodsField.setAccessible(true);
        httpMethods = (Set<HttpMethod>) methodsField.get(routeState);

      } catch (NoSuchFieldException | IllegalAccessException e) {

        /**
         * Vertx 3.5.0
         */
        try {
          Field methods = route.getClass().getDeclaredField("methods");
          methods.setAccessible(true);
          httpMethods = (Set<HttpMethod>) methods.get(route);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
          throw new RuntimeException(ex);
        }

      }
      this.httpMethodsPathItemMap = httpMethods.stream().collect(Collectors.toMap(e -> e, e -> new PathItem()));
    }


    public Set<PathItem> getPathApis() {
      return new HashSet<>(this.httpMethodsPathItemMap.values());
    }

    /**
     * Set the OpenAPI {@link Operation operations}
     * from the methods set on the {@link Route vertx Route}
     *
     * @return
     */
    public RouteWrapper createOpenApiOperationFromRouteMethod() {


      for (HttpMethod httpMethod : getHttpMethods()) {

        PathItem pathItem = httpMethodsPathItemMap.get(httpMethod);

        /**
         * Operation creation
         */
        Operation operation = new Operation();
        operation.setParameters(extractOperationParametersFromPath(route.getPath()));

        /**
         * Set the operation to the good pathItem method
         */
        PathItem.HttpMethod openApiHttpMethod = PathItem.HttpMethod.valueOf(httpMethod.name());
        switch (openApiHttpMethod) {
          case TRACE:
            pathItem.trace(operation);
            break;
          case PUT:
            pathItem.put(operation);
            break;
          case POST:
            pathItem.post(operation);
            break;
          case PATCH:
            pathItem.patch(operation);
            break;
          case GET:
            pathItem.get(operation);
            break;
          case OPTIONS:
            pathItem.options(operation);
            break;
          case HEAD:
            pathItem.head(operation);
            break;
          case DELETE:
            pathItem.delete(operation);
            break;
          default:
            throw new RuntimeException("Unknown operation method (" + openApiHttpMethod + ")");
        }
      }
      return this;
    }

    public RouteWrapper setOperationsAttributesFromHandlersAnnotations() {


      for (Class<?> handlerClass : getHandlerClasses()) {

        for (Method method : handlerClass.getDeclaredMethods()) {

          io.swagger.v3.oas.annotations.Operation annotation = method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class);
          if (annotation != null) {

            String httpMethodName = annotation.method();
            PathItem pathItem;
            try {
              pathItem = this.getPathItemFromHttpMethodName(httpMethodName);
            } catch (NameNotFoundException e) {
              /**
               * When the handler methods are packaged into one class
               */
              continue;
            }
            Operation matchedOperation;
            switch (PathItem.HttpMethod.valueOf(httpMethodName.toUpperCase())) {
              case TRACE:
                matchedOperation = pathItem.getTrace();
                break;
              case PUT:
                matchedOperation = pathItem.getPut();
                break;
              case POST:
                matchedOperation = pathItem.getPost();
                break;
              case PATCH:
                matchedOperation = pathItem.getPatch();
                break;
              case GET:
                matchedOperation = pathItem.getGet();
                break;
              case OPTIONS:
                matchedOperation = pathItem.getOptions();
                break;
              case HEAD:
                matchedOperation = pathItem.getHead();
                break;
              case DELETE:
                matchedOperation = pathItem.getDelete();
                break;
              default:
                throw new RuntimeException("The method parameter of the OpenAPI operation should be present in the handler (" + handlerClass + ")");
            }

            if (matchedOperation == null) {
              throw new RuntimeException("The method handler (" + method.getDeclaringClass().getSimpleName() + "::" + method.getName() + ") has a method annotation (" + httpMethodName + ") that is not define in the router.");
            }
            /**
             * Process Method Annotation
             */
            OpenApiAnnotationMappers.mergeAnnotationOperationIntoOpenApiOperation(annotation, matchedOperation);

            /**
             * Request Body in Method Parameter Annotation
             */
            RequestBody body = method.getParameters()[0].getAnnotation(RequestBody.class);
            if (body != null) {
              matchedOperation.setRequestBody(OpenApiAnnotationMappers.requestBodyAnnotationToOpenApi(body));
            }

          }
        }

      }
      return this;
    }


    private PathItem getPathItemFromHttpMethodName(String httpMethodName) throws NameNotFoundException {
      HttpMethod httpMethod = this.httpMethodsPathItemMap.keySet().stream().filter(e -> e.name().toLowerCase().equals(httpMethodName.toLowerCase())).findFirst().orElse(null);
      if (httpMethod == null) {
        throw new NameNotFoundException("The method name (" + httpMethodName + ") was not found on the route (" + route.getPath() + ")");
      }
      return this.httpMethodsPathItemMap.get(httpMethod);
    }

    public boolean isNotOpenApiRoute() {
      return !getHandlerClasses().contains(OpenApiHandler.class);
    }

  }
}
