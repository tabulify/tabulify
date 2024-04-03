package net.bytle.tower.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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
        stringBuilder.append("<body>");
        break;
      case "p":
        stringBuilder.append("<p>");
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
}
