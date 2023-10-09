package net.bytle.email;

import net.bytle.template.ThymeleafResolverBuilder;
import net.bytle.template.ThymeleafTemplateEngine;
import net.bytle.template.api.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;

public class BMailTransactionalLocalTemplateEngine {


  public static final String EMAIL_TEMPLATES_RESOURCE_DIRECTORY_LOCATION_DEFAULT = "/email/templates/transactional/";
  private static final TemplateEngine templateEngine;

  static {
    templateEngine = BMailTransactionalLocalTemplateEngine
      .config().build();
  }

  public static TemplateEngine getDefault() {

    return templateEngine;

  }

  public static config config() {
    return new config();
  }

  public static class config {
    private Boolean cache = true;

    public config setCache(Boolean cache) {
      this.cache = cache;
      return this;
    }

    public TemplateEngine build() {

      org.thymeleaf.TemplateEngine templateEngine = new org.thymeleaf.TemplateEngine();

      String resourceTemplateBasePath = EMAIL_TEMPLATES_RESOURCE_DIRECTORY_LOCATION_DEFAULT;

      /**
       * Email HTML template
       */
      AbstractConfigurableTemplateResolver htmlTemplateResolver = ThymeleafResolverBuilder
        .create()
        .setInModuleFromClass(BMailTransactionalLocalTemplateEngine.class)
        .setResourcePath(resourceTemplateBasePath)
        .setCache(cache)
        .setTemplateMode(TemplateMode.HTML)
        .setOrder(1)
        .build();
      templateEngine.addTemplateResolver(htmlTemplateResolver);

      /**
       * Email Txt template
       */
      AbstractConfigurableTemplateResolver txtFileTemplateResolver = ThymeleafResolverBuilder
        .create()
        .setInModuleFromClass(BMailTransactionalLocalTemplateEngine.class)
        .setResourcePath(resourceTemplateBasePath)
        .setCache(cache)
        .setTemplateMode(TemplateMode.TEXT)
        .setOrder(2)
        .build();
      templateEngine.addTemplateResolver(txtFileTemplateResolver);


      return ThymeleafTemplateEngine.createFromThymeleafEngine(templateEngine);

    }
  }
}
