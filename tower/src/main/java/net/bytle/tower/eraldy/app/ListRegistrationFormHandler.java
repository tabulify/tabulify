package net.bytle.tower.eraldy.app;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.IllegalStructure;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ListRegistrationFormHandler implements Handler<RoutingContext> {
  public static final String URI_PATH = "/register/list";
  public static final String LIST_GUID_PARAM = "listGuid";

  private static final Logger LOGGER = LoggerFactory.getLogger(ListRegistrationFormHandler.class);


  @Override
  public void handle(RoutingContext routingContext) {

    Vertx vertx = routingContext.vertx();
    String listGuidParam = LIST_GUID_PARAM;
    String listGuid = routingContext.request().getParam(listGuidParam);
    if (listGuid == null) {
      String requestUri = routingContext.request().absoluteURI();
      String exampleRequestUri = null;
      try {
        UriEnhanced uriEnhanced = UriEnhanced.createFromString(requestUri);
        exampleRequestUri = uriEnhanced.addQueryProperty(listGuidParam, "xjKlMk")
          .toUrl()
          .toString();
      } catch (IllegalStructure e) {
        LOGGER.warn("The request Uri (" + requestUri + ") could not be parsed by UriEnhanced.", e);
      }
      Map<String, Object> variables = new HashMap<>();
      variables.put("title", "The list guid is mandatory as parameter.");
      String message = "You should add the <mark>" + listGuidParam + "</mark> parameter to the URL.";
      if (exampleRequestUri != null) {
        message += "<br><br><b>Example:</b>";
        message += "<br>" + exampleRequestUri;
      }
      variables.put("message", message);
      String errorHtml = TemplateEngine.getLocalHtmlEngine(vertx)
        .compile("Error.html")
        .applyVariables(variables)
        .getResult();
      routingContext
        .response()
        .putHeader("Content-Type", "text/html")
        .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
        .end(errorHtml);
    }



  }

}
