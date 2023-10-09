package net.bytle.tower.util;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class JsonUtil {

  protected static final Logger LOGGER = LoggerFactory.getLogger(JsonUtil.class);

  /**
   * @param context
   * @param page
   * @param expectedKeys
   * @return Example:
   * JsonObject json = context.getBodyAsJson();
   * if (!validateJsonPageDocument(context, json, "key1", "key2")) {
   * return;
   * }
   */
  private boolean validateJsonPageDocument(RoutingContext context, JsonObject page, String... expectedKeys) {
    if (!Arrays.stream(expectedKeys).allMatch(page::containsKey)) {
      LOGGER.error("Bad page creation JSON payload: " + page.encodePrettily() + " from " + context.request().remoteAddress());
      context.response().setStatusCode(400);
      context.response().putHeader("Content-Type", "application/json");
      context.response().end(new JsonObject()
        .put("success", false)
        .put("error", "Bad request payload").encode());
      return false;
    }
    return true;
  }

}
