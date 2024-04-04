package net.bytle.tower.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bytle.java.JavaEnvs;

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
   *
   * @param jsonObject        - the AST
   * @param htmlStringBuilder - the HTML string builder
   */
  private void toHTMLAst(JsonObject jsonObject, StringBuilder htmlStringBuilder) {

    String tag = jsonObject.getString("tag");
    if (tag == null) {
      String text = jsonObject.getString("text");
      if (text == null) {
        return;
      }
      Map<String, String> styles = new HashMap<>();
      boolean isBold = jsonObject.containsKey("bold");
      if (isBold) {
        styles.put("font-weight", "bold");
      }
      boolean isItalic = jsonObject.containsKey("italic");
      if (isItalic) {
        styles.put("font-style", "italic");
      }
      boolean isUnderline = jsonObject.containsKey("underline");
      if (isUnderline) {
        styles.put("text-decoration", "underline");
      }
      if (styles.isEmpty()) {
        htmlStringBuilder.append(text);
        return;
      }
      HashMap<String, String> attributes = new HashMap<>();
      String style = toHTMLStyleAttribute(styles);
      attributes.put("style", style);
      addHTMLEnterTag("span", attributes, htmlStringBuilder);
      htmlStringBuilder
        .append(text)
        .append("</span>");
      return;
    }

    /**
     * Open Tag
     */
    String htmlTag = tag;
    switch (tag) {
      case "a":
        Map<String, String> anchorAttributes = new HashMap<>();
        String url = jsonObject.getString("url");
        if (url != null) {
          anchorAttributes.put("href", url);
        }
        String title = jsonObject.getString("title");
        if (title != null) {
          anchorAttributes.put("title", title);
        }
        addHTMLEnterTag(htmlTag, anchorAttributes, htmlStringBuilder);
        break;
      case "body":
      case "p":
      case "h1":
      case "h2":
      case "h3":
      case "h4":
      case "li":
      case "ul":
      case "ol":
        addHTMLEnterTag(tag, new HashMap<>(), htmlStringBuilder);
        break;
      default:
        htmlTag = null;
        if (JavaEnvs.IS_DEV) {
          htmlTag = "span";
          Map<String, String> styles = new HashMap<>();
          styles.put("color", "red");
          Map<String, String> unknownTagAttributes = new HashMap<>();
          unknownTagAttributes.put("style", toHTMLStyleAttribute(styles));
          addHTMLEnterTag(htmlTag, unknownTagAttributes, htmlStringBuilder);
          htmlStringBuilder.append("Internal Error: Tag (").append(tag).append(") is unknown. ");
        }
        break;
    }

    JsonArray children = jsonObject.getJsonArray("children");
    if (children != null) {
      for (int i = 0; i < children.size(); i++) {
        Object arrayElement = children.getValue(i);
        if (arrayElement instanceof JsonObject) {
          toHTMLAst((JsonObject) arrayElement, htmlStringBuilder);
        } else {
          htmlStringBuilder.append("This children is not a object");
        }
      }
    }

    /**
     * Close
     */
    if (htmlTag != null) {
      htmlStringBuilder.append("</").append(htmlTag).append(">");
    }


  }

  private String toHTMLStyleAttribute(Map<String, String> stylesProperties) {
    StringBuilder stylesStringBuilder = new StringBuilder();
    for (Map.Entry<String, String> style : stylesProperties.entrySet()) {
      stylesStringBuilder
        .append(style.getKey())
        .append(": ")
        .append(style.getValue())
        .append(";");
    }
    return stylesStringBuilder.toString();
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
