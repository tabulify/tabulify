package net.bytle.vertx;

import net.bytle.email.BMailTransactionalLocalTemplateEngine;
import net.bytle.template.ThymeleafResolverBuilder;
import net.bytle.template.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;

import static net.bytle.vertx.HtmlUtil.WEBROOT;

/**
 * Template Engine wrapper for vertx
 * It manages the template origin and the configuration (cache or not)
 */
public class TemplateEngines {

  private static final Logger LOGGER = LoggerFactory.getLogger(TemplateEngines.class);

  private final TemplateEngine emailEngine;
  private final TemplateEngine localHtmlEngine;
  private static final String TEMPLATE_CACHE_CONF = "template.cache";
  public static final String TEMPLATES_RESOURCE_ROOT = "/" + WEBROOT + "/";
  private final Boolean cache;

  protected TemplateEngines(Server server) {

    Boolean templateCacheConf = server.getConfigAccessor().getBoolean(TEMPLATE_CACHE_CONF);
    if (templateCacheConf == null) {
      throw new IllegalArgumentException("The cache template configuration is mandatory. You can set set in the conf with the attribute (" + TEMPLATE_CACHE_CONF + ")");
    } else {
      LOGGER.info("Configuration: The template cache conf was found and set to " + templateCacheConf);
    }
    this.cache = templateCacheConf;

    /**
     * Email template Engine
     */
    net.bytle.template.api.TemplateEngine thymeleafEmailTemplateEngine = BMailTransactionalLocalTemplateEngine
      .config()
      .setCache(this.cache)
      .build();
    thymeleafEmailTemplateEngine.clearCache();
    emailEngine = new TemplateEngine(thymeleafEmailTemplateEngine);

    /**
     * HTML, local
     */
    net.bytle.template.api.TemplateEngine thymeleafLocalTemplateEngine = this.getTemplateEngineWithLocalTemplateResourceResolver();
    thymeleafLocalTemplateEngine.clearCache();

    localHtmlEngine = new TemplateEngine(thymeleafLocalTemplateEngine);


  }


  /**
   * @return the email engine of the email library configured
   */
  public net.bytle.template.api.TemplateEngine getEmailEngine() {

    return emailEngine;
  }

  /**
   * @return the engine based on the local path
   */
  public net.bytle.template.api.TemplateEngine getLocalHtmlEngine() {

    return localHtmlEngine;
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
