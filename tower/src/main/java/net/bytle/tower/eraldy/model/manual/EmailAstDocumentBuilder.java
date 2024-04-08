package net.bytle.tower.eraldy.model.manual;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bytle.tower.util.RichSlateAST;

/**
 * Build a Rich Slate HTML AST for an email
 */
public class EmailAstDocumentBuilder {


  private String documentTitle;
  private String documentLanguage;
  private String emailPreview;
  private JsonArray body;

  public static EmailAstDocumentBuilder create() {
    return new EmailAstDocumentBuilder();
  }

  public EmailAstDocumentBuilder setTitle(String title) {
    this.documentTitle = title;
    return this;
  }

  public EmailAstDocumentBuilder setLanguage(String language) {
    this.documentLanguage = language;
    return this;
  }

  /**
   * @return Build an email HTML AST
   */
  public JsonObject build() {

    String tag = RichSlateAST.TAG_ATTRIBUTE;

    /**
     * Email body
     */
    JsonArray body = new JsonArray();
    if (emailPreview != null) {
      body.add(new JsonObject()
        .put(tag, RichSlateAST.PREVIEW_TAG)
        .put("content", emailPreview)
      );
    }
    body.addAll(this.body);

    /**
     * HTML document building
     */
    String children = RichSlateAST.CHILDREN;
    return new JsonObject()
      .put(tag, "html")
      .put("xmlns", "http://www.w3.org/1999/xhtml")
      .put("lang", this.documentLanguage)
      .put(children, new JsonArray()
        .add(new JsonObject()
          .put(tag, "head")
          .put(children, new JsonArray()
            .add(new JsonObject()
              .put(tag, "meta")
              .put("name", "viewport")
              .put("content", "width=device-width")
            )
            .add(new JsonObject()
              .put(tag, "meta")
              .put("http-equiv", "Content-Type")
              .put("content", "text/html; charset=UTF-8")
            )
            .add(new JsonObject()
              .put(tag, "title")
              .put(children, new JsonArray()
                .add(new JsonObject()
                  .put("text", this.documentTitle)
                ))
            )
          ))
        .add(new JsonObject()
          .put(tag, "body")
          .put(children, body))
      );
  }

  /**
   * @param emailPreview - the preview in a text format (text)
   */
  public EmailAstDocumentBuilder setPreview(String emailPreview) {
    this.emailPreview = emailPreview;
    return this;
  }

  /**
   *
   * @param body - the content (ie Rs AST in a Rich Slate)
   */
  public EmailAstDocumentBuilder setBody(JsonArray body) {
    this.body = body;
    return this;
  }
}
