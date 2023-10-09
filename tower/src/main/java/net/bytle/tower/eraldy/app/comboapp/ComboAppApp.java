package net.bytle.tower.eraldy.app.comboapp;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.RouterBuilder;
import net.bytle.tower.TowerApexDomain;
import net.bytle.tower.TowerApp;

/**
 * The combo front end
 */
public class ComboAppApp extends TowerApp {

  public static final String COMBO_NAME = "combo";

  public ComboAppApp(TowerApexDomain topLevelDomain) {
    super(topLevelDomain);
  }

  public static ComboAppApp create(TowerApexDomain topLevelDomain) {
    return new ComboAppApp(topLevelDomain);
  }

  @Override
  public String getAppName() {
    return COMBO_NAME;
  }

  @Override
  public TowerApp openApiMount(RouterBuilder builder) {
    return this;
  }

  @Override
  public TowerApp openApiBindSecurityScheme(RouterBuilder builder, JsonObject jsonConfig) {
    return this;
  }

  @Override
  protected String getPublicSubdomainName() {
    return COMBO_NAME;
  }

  @Override
  protected TowerApp addSpecificAppHandlers(Router router) {
    String apiAbsolutePath = this.getAbsoluteLocalPathWithDomain() + "/*";
    /**
     * Add login
     */
    router.route(apiAbsolutePath)
      .handler(new ComboAppLoginHandler(this));
    return this;
  }

  @Override
  public boolean hasOpenApiSpec() {
    return false;
  }

  @Override
  public String getPublicDefaultOperationPath() {
    return "";
  }

  @Override
  protected String getPublicAbsolutePathMount() {
    return "";
  }

  @Override
  public boolean getIsHtmlApp() {
    return true;
  }

  @Override
  public boolean isSocial() {
    return false;
  }

}
