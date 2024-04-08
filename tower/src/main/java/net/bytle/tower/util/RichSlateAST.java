package net.bytle.tower.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bytle.exception.InternalException;
import net.bytle.java.JavaEnvs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A class to print a Rich Slate AST
 * created by our Rich Slate Editor to another format
 */
public class RichSlateAST {


  public static final String TAG_ATTRIBUTE = "tag";

  public static final String PREVIEW_TAG = "preview";
  public static final String CHILDREN = "children";
  private static final List<String> COMMON_ATTRIBUTES = Arrays.asList(TAG_ATTRIBUTE, CHILDREN);
  private static final String HTML = "html";
  private static final String TEXT = "text";

  private final Builder builder;

    /**
   * The number for ordered list
   */
  private int listNumber = 0;
  /**
   * The type of list (ul, ol)
   */
  private String listType;

  /**
   * To keep the URL in a text anchor to create at the
   * end the markdown [label](url)
   */
  private String url;

  public RichSlateAST(Builder builder) {
      this.builder = builder;
  }

  /**
   * FormInput are for one line content with possible variable
   *
   * @param emailRsAst - the AST
   * @return a text without any carriage return
   */
  public static Builder createFromFormInputAst(String emailRsAst) {
    JsonArray jsonArray = new JsonArray(emailRsAst);
    JsonObject paragraphJsonObject = jsonArray.getJsonObject(0);
    return new Builder()
      .setNoNewLine(true)
      .setDocument(paragraphJsonObject);
  }


  public String toEmailHTML() {

    return toFormat(HTML);

  }


  /**
   * A recursive function that will build a format string in the string builder
   *
   * @param jsonObject        - the AST
   * @param format            - the format
   */
  private void traverse(JsonObject jsonObject, String format, StringBuilder stringBuilder) {


    String tag = jsonObject.getString("tag");

    /**
     * Leaf
     */
    if (tag == null) {
      this.renderLeaf(jsonObject, format, stringBuilder);
      return;
    }


    /**
     * Element
     */
    this.renderElement(jsonObject, format, stringBuilder);


  }

  private void renderElement(JsonObject jsonObject, String format, StringBuilder stringBuilder) {

    String tag = jsonObject.getString("tag");

    /**
     * Self-Closing/Empty element
     */
    if (tag.equals("variable")) {
      String variableId = jsonObject.getString("variableId");
      String value = this.builder.variables.getString(variableId);
      if (value != null) {
        stringBuilder.append(value);
      }
      return;
    }

    /**
     * Open/Close Element
     */
    String htmlTag = tag;
    switch (tag) {
      case PREVIEW_TAG:
        /**
         * Only for HTML
         */
        if (!format.equals(HTML)) {
          return;
        }
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
        addHTMLEnterTag("span", attributes, stringBuilder);
        stringBuilder
          .append(content)
          .append("</span>");
        return;
      case "a":
        JsonObject anchorAttributes = new JsonObject();
        String url = jsonObject.getString("url");
        String title = jsonObject.getString("title");
        switch (format) {
          case HTML:
            if (url != null) {
              anchorAttributes.put("href", url);
            }
            if (title != null) {
              anchorAttributes.put("title", title);
            }
            addHTMLEnterTag(htmlTag, anchorAttributes, stringBuilder);
            break;
          default:
            // text
            // keep the url to create the markdown link at the end
            this.url = url;
            stringBuilder.append("[");
            break;
        }
        break;
      case "ul":
      case "ol":
        switch (format) {
          case HTML:
            addHTMLEnterTag(tag, new JsonObject(), stringBuilder);
            break;
          default:
            // text
            this.listNumber = 0;
            this.listType = tag;
            break;
        }
        break;
      case "p":
      case "body":
      case "h1":
      case "h2":
      case "h3":
      case "h4":
      case "html":
      case "title":
      case "head":
      case "meta":
        if (format.equals(HTML)) {
          addHTMLEnterTag(htmlTag, this.getProps(jsonObject), stringBuilder);
        }
        break;
      case "li":
        switch (format) {
          case HTML:
            addHTMLEnterTag(htmlTag, this.getProps(jsonObject), stringBuilder);
            break;
          default:
            // text
            switch (listType) {
              case "ul":
                stringBuilder.append("  * ");
                break;
              case "ol":
                this.listNumber++;
                stringBuilder.append("  ").append(listNumber).append(". ");
                break;
            }
            break;
        }
        break;
      default:
        htmlTag = tag;
        switch (format) {
          case HTML:
            if (JavaEnvs.IS_DEV) {
              htmlTag = "span";
              JsonObject unknownTagAttributes = new JsonObject()
                .put("style", toHTMLStyleAttribute(new JsonObject().put("color", "red")));
              addHTMLEnterTag(htmlTag, unknownTagAttributes, stringBuilder);
            } else {
              addHTMLEnterTag(htmlTag, this.getProps(jsonObject), stringBuilder);
            }
            break;
        }
        stringBuilder.append("Internal Error: Tag (").append(tag).append(") is unknown. ");
        break;
    }

    JsonArray children = jsonObject.getJsonArray(CHILDREN);
    if (children != null) {
      // The meta tag can happen when we transform a full AST with HTML meta to text
      boolean isMetaTag = Arrays.asList("title", "meta", "link", "script").contains(tag);
      if (!(format.equals(TEXT) & isMetaTag)) {
        for (int i = 0; i < children.size(); i++) {
          Object arrayElement = children.getValue(i);
          if (arrayElement instanceof JsonObject) {
            traverse((JsonObject) arrayElement, format, stringBuilder);
          } else {
            stringBuilder.append("This child is not a object");
          }
        }
      }
    }

    /**
     * Close
     */
    switch (format) {
      case HTML:
        stringBuilder.append("</").append(htmlTag).append(">");
        break;
      default:
        // text
        switch (tag) {
          case "ol":
          case "ul":
          case "p":
            if (!this.builder.noNewLine) {
              stringBuilder.append("\n\n");
            }
            break;
          case "li":
            if (!this.builder.noNewLine) {
              stringBuilder.append("\n");
            }
            break;
          case "a":
            stringBuilder.append("](").append(this.url).append(")");
            break;
        }
    }
  }

  private void renderLeaf(JsonObject jsonObject, String format, StringBuilder stringBuilder) {
    String text = jsonObject.getString(TEXT);
    if (text == null) {
      return;
    }
    boolean isBold = jsonObject.containsKey("bold");
    boolean isItalic = jsonObject.containsKey("italic");
    boolean isUnderline = jsonObject.containsKey("underline");
    switch (format) {
      case HTML:
        JsonObject styles = new JsonObject();
        if (isBold) {
          styles.put("font-weight", "bold");
        }
        if (isItalic) {
          styles.put("font-style", "italic");
        }
        if (isUnderline) {
          styles.put("text-decoration", "underline");
        }
        if (styles.isEmpty()) {
          stringBuilder.append(text);
          return;
        }
        JsonObject attributes = new JsonObject()
          .put("style", toHTMLStyleAttribute(styles));
        addHTMLEnterTag("span", attributes, stringBuilder);
        stringBuilder
          .append(text)
          .append("</span>");
        return;
      default:
        // text
        if (isBold) {
          stringBuilder.append("**");
        }
        if (isItalic) {
          stringBuilder.append("__");
        }
        stringBuilder.append(text);
        if (isItalic) {
          stringBuilder.append("__");
        }
        if (isBold) {
          stringBuilder.append("**");
        }
    }
  }

  private JsonObject getProps(JsonObject jsonObject) {
    JsonObject propertiesJson = new JsonObject();
    for (Map.Entry<String, Object> entry : jsonObject.getMap().entrySet()) {
      if (COMMON_ATTRIBUTES.contains(entry.getKey())) {
        continue;
      }
      propertiesJson.put(entry.getKey(), entry.getValue());
    }
    return propertiesJson;
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

  public String toEmailText() {


    return toFormat(TEXT);

  }

  private String toFormat(String format) {
    StringBuilder stringBuilder = new StringBuilder();
    this.traverse(this.builder.document, format, stringBuilder);
    return stringBuilder.toString();
  }

  public static class Builder {
    private JsonObject document;
    private boolean noNewLine = false;
    private JsonObject variables = new JsonObject();

    public Builder() {

    }

    /**
     * @param document - the document
     */
    public Builder setDocument(JsonObject document) {
      this.document = document;
      return this;
    }

    public Builder setNoNewLine(boolean b) {
      this.noNewLine = b;
      return this;
    }


    public Builder addVariables(JsonObject variables) {
      this.variables = variables;
      return this;
    }




    public RichSlateAST build() {
      return new RichSlateAST(this);
    }
  }
}
