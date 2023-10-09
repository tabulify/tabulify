package net.bytle.tower.util;

import io.vertx.core.Vertx;
import net.bytle.email.BMailTransactionalLocalTemplateEngine;
import net.bytle.exception.InternalException;
import net.bytle.template.ThymeleafResolverBuilder;
import net.bytle.template.ThymeleafTemplateEngine;
import net.bytle.template.api.Template;
import net.bytle.vertx.ConfigAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;

import java.util.HashMap;
import java.util.Map;

import static net.bytle.tower.util.HtmlUtil.WEBROOT;

/**
 * Template Engine wrapper for vertx
 * It manages the template origin and the configuration (cache or not)
 */
public class TemplateEngine implements net.bytle.template.api.TemplateEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(TemplateEngine.class);

  static private final Map<Vertx, TemplateEngine> templateEngineEmailMap = new HashMap<>();
  static private final Map<Vertx, TemplateEngine> templateLocalMap = new HashMap<>();
  private static final String TEMPLATE_CACHE_CONF = "template.cache";
  public static final String TEMPLATES_RESOURCE_ROOT = "/" + WEBROOT + "/";
  private final net.bytle.template.api.TemplateEngine templateEngine;

  private TemplateEngine(net.bytle.template.api.TemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }


  /**
   * @param vertx - the vertx
   * @return the email engine of the email library configured
   */
  public static net.bytle.template.api.TemplateEngine getEmailEngine(Vertx vertx) {
    TemplateEngine templateEngineLocal = templateEngineEmailMap.get(vertx);
    if (templateEngineLocal == null) {
      throw new InternalException("Email Template engine should not be null at this point because it's instantiated at the start of the verticle");
    }
    return templateEngineLocal;
  }

  /**
   * @param vertx - the vertx
   * @return the engine based on the local path
   */
  public static net.bytle.template.api.TemplateEngine getLocalHtmlEngine(Vertx vertx) {
    TemplateEngine templateEngineLocal = templateLocalMap.get(vertx);
    if (templateEngineLocal == null) {
      throw new InternalException("Local Template engine should not be null at this point because it's instantiated at the start of the verticle");
    }
    return templateEngineLocal;
  }

  public static config config(Vertx vertx, ConfigAccessor jsonConfig) {
    Boolean templateCacheConf = jsonConfig.getBoolean(TEMPLATE_CACHE_CONF);
    if (templateCacheConf == null) {
      throw new InternalException("The template cache configuration was not found. You need to set it via the " + TEMPLATE_CACHE_CONF + " attribute.");
    } else {
      LOGGER.info("Configuration: The template cache conf was found and set to " + templateCacheConf);
    }
    return new config(vertx)
      .setCache(templateCacheConf);
  }

  @Override
  public Template compile(String templatePathFromResources) {

    return templateEngine.compile(templatePathFromResources);

  }

  @Override
  public void clearCache() {
    this.templateEngine.clearCache();
  }

  public static class config {
    private final Vertx vertx;
    private Boolean cache;


    config(Vertx vertx) {
      this.vertx = vertx;
    }

    public config setCache(Boolean cache) {
      this.cache = cache;
      return this;
    }

    /**
     * Instantie the template engine
     */
    public void create() {

      /**
       * Cache
       */
      if (this.cache == null) {
        throw new IllegalArgumentException("The cache template configuration is mandatory. You can set set in the conf with the attribute (" + TEMPLATE_CACHE_CONF + ")");
      }

      /**
       * Email template Engine
       */
      net.bytle.template.api.TemplateEngine thymeleafEmailTemplateEngine = BMailTransactionalLocalTemplateEngine
        .config()
        .setCache(this.cache)
        .build();
      thymeleafEmailTemplateEngine.clearCache();
      TemplateEngine towerEmailTemplateEngine = new TemplateEngine(thymeleafEmailTemplateEngine);
      templateEngineEmailMap.put(vertx, towerEmailTemplateEngine);

      /**
       * HTML, local
       */
      net.bytle.template.api.TemplateEngine thymeleafLocalTemplateEngine = this.getTemplateEngineWithLocalTemplateResourceResolver();
      thymeleafLocalTemplateEngine.clearCache();

      TemplateEngine templateEngine = new TemplateEngine(thymeleafLocalTemplateEngine);
      templateLocalMap.put(vertx, templateEngine);

    }

    private net.bytle.template.api.TemplateEngine getTemplateEngineWithLocalTemplateResourceResolver() {

      org.thymeleaf.TemplateEngine templateEngine = new org.thymeleaf.TemplateEngine();

      /**
       * Common configuration HTML
       */
      AbstractConfigurableTemplateResolver htmlTemplateResolver = ThymeleafResolverBuilder
        .create()
        .setCache(cache)
        .setResourcePath(TEMPLATES_RESOURCE_ROOT)
        .setOrder(1)
        .setTemplateMode(TemplateMode.HTML)
        .build();
      templateEngine.addTemplateResolver(htmlTemplateResolver);

      /**
       * Javascript
       */
      AbstractConfigurableTemplateResolver jsTemplateResolver = ThymeleafResolverBuilder
        .create()
        .setCache(cache)
        .setResourcePath(TEMPLATES_RESOURCE_ROOT)
        .setOrder(2)
        .setTemplateMode(TemplateMode.JAVASCRIPT)
        .build();
      templateEngine.addTemplateResolver(jsTemplateResolver);

      return ThymeleafTemplateEngine.createFromThymeleafEngine(templateEngine);

    }


  }
}
