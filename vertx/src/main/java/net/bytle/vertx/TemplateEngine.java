package net.bytle.vertx;

import net.bytle.template.api.Template;

public class TemplateEngine implements net.bytle.template.api.TemplateEngine {


  private final net.bytle.template.api.TemplateEngine engine;

  public TemplateEngine(net.bytle.template.api.TemplateEngine templateEngine) {
    this.engine = templateEngine;
  }

  @Override
  public Template compile(String string) {
    return this.engine.compile(string);
  }

  @Override
  public void clearCache() {
    this.engine.clearCache();
  }
}
