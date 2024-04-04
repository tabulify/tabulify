package net.bytle.tower.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * A class to print a Rich Slate AST
 * created by our Rich Slate Editor to another format
 */
public class RichSlateAST {


  private final JsonArray richSlateAst;

  public RichSlateAST(JsonArray richSlateAst) {
    this.richSlateAst = richSlateAst;
  }

  public static RichSlateAST ofJsonString(String jsonString) {

    return new RichSlateAST(new JsonArray(jsonString));

  }

  public String toEmailHTML() {


    StringBuilder stringBuilder = new StringBuilder();
    JsonObject bodyTag = new JsonObject();
    bodyTag.put("tag", "body");
    bodyTag.put("children", this.richSlateAst);
    this.toHTMLAst(bodyTag, stringBuilder);
    return stringBuilder.toString();
  }

  /**
   * A recursive function that will build an HTML string in the string builder
   * @param jsonObject - the AST
   * @param stringBuilder - the HTML string builder
   */
  private void toHTMLAst(JsonObject jsonObject, StringBuilder stringBuilder) {

    String tag = jsonObject.getString("tag");
    if (tag == null) {
      String text = jsonObject.getString("text");
      if (text != null) {
        stringBuilder.append(text);
      }
      return;
    }

      switch (tag) {
      case "body":
      case "p":
        addHTMLEnterTag(tag, new HashMap<>(), stringBuilder);
        break;
      case "a":
        String url = jsonObject.getString("url");
        String title = jsonObject.getString("title");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("href", url);
        attributes.put("title", title);
        addHTMLEnterTag(tag, attributes, stringBuilder);
        break;
    }

    JsonArray children = jsonObject.getJsonArray("children");
    if (children != null) {
      for (int i = 0; i < children.size(); i++) {
        Object arrayElement = children.getValue(i);
        if (arrayElement instanceof JsonObject) {
          toHTMLAst((JsonObject) arrayElement, stringBuilder);
        } else {
          stringBuilder.append("This children is not a object");
        }
      }
    }

    /**
     * Close
     */
    stringBuilder.append("</").append(tag).append(">");


  }

  private void addHTMLEnterTag(String tag, Map<String, String> attributes, StringBuilder stringBuilder) {
    stringBuilder
      .append("<")
      .append(tag);
    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      stringBuilder
        .append(" ")
        .append(attribute.getKey())
        .append("=\"")
        .append(attribute.getValue())
        .append("\"");
    }
    stringBuilder.append(">");
  }
}
