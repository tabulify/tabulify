package net.bytle.tower.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bytle.exception.InternalException;
import net.bytle.java.JavaEnvs;

import java.util.Map;

/**
 * A class to print a Rich Slate AST
 * created by our Rich Slate Editor to another format
 */
public class RichSlateAST {


  public static final String TAG_ATTRIBUTE = "tag";

  public static final String PREVIEW_TAG = "preview";
  private final JsonObject richSlateAst;

  public RichSlateAST(JsonObject richSlateAst) {
    this.richSlateAst = richSlateAst;
  }


  public String toEmailHTML() {

    StringBuilder stringBuilder = new StringBuilder();
    this.toHTMLAst(richSlateAst, stringBuilder);
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
      JsonObject styles = new JsonObject();
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
      JsonObject attributes = new JsonObject()
        .put("style", toHTMLStyleAttribute(styles));
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
      case PREVIEW_TAG:
        String content = jsonObject.getString("content");
        if (content == null) {
          if (JavaEnvs.IS_DEV) {
            throw new InternalException("A preview tag should have a content attribute");
          }
          return;
        }
        // preheader is the selector class name for client email for preview
        JsonObject attributes = new JsonObject()
          .put("class", "preheader")
          .put("style", toHTMLStyleAttribute(new JsonObject()
            .put("color", "transparent")
            .put("display", "none")
            .put("height", "0")
            .put("max-height", "0")
            .put("max-width", "0")
            .put("opacity", "0")
            .put("overflow", "hidden")
            .put("mso-hide", "all")
            .put("visibility", "hidden")
            .put("width", "0")
          ));
        addHTMLEnterTag("span", attributes, htmlStringBuilder);

        htmlStringBuilder
          .append(content)
          .append("</span>");
        return;
      case "a":
        JsonObject anchorAttributes = new JsonObject();
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
        addHTMLEnterTag(tag, new JsonObject(), htmlStringBuilder);
        break;
      default:
        htmlTag = null;
        if (JavaEnvs.IS_DEV) {
          htmlTag = "span";
          JsonObject unknownTagAttributes = new JsonObject()
            .put("style", toHTMLStyleAttribute(new JsonObject().put("color", "red")));
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

  private String toHTMLStyleAttribute(JsonObject stylesProperties) {
    StringBuilder stylesStringBuilder = new StringBuilder();
    for (Map.Entry<String, Object> style : stylesProperties.getMap().entrySet()) {
      stylesStringBuilder
        .append(style.getKey())
        .append(": ")
        .append(style.getValue())
        .append(";");
    }
    return stylesStringBuilder.toString();
  }

  private void addHTMLEnterTag(String tag, JsonObject attributes, StringBuilder stringBuilder) {
    stringBuilder
      .append("<")
      .append(tag);
    for (Map.Entry<String, Object> attribute : attributes.getMap().entrySet()) {
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
