package net.bytle.tower.eraldy.api.implementer.letter;

import io.vertx.ext.web.RoutingContext;
import net.bytle.template.api.Template;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.vertx.TemplateEngines;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * The letter (in HTML format)
 * that is show to the user when he/she validate
 * a subscription by clicking on the validation link
 * send by email.
 */
public class ListRegistrationConfirmationLetter {


  /**
   * If the Get link has a debug parameter, all template variables/data are printed
   */
  private static final String URI_DEBUG_PARAMETER = "debug";
  /**
   * The path from the {@link TemplateEngines#TEMPLATES_RESOURCE_ROOT template resources directory}
   */
  public static final String TEMPLATE_NAME = "list-registration-confirmation-letter.html";

  /**
   * The data passed to the template
   */
  private final HashMap<String,Object> data;

  /**
   * The template
   */
  private final Template htmlTemplate;

  public ListRegistrationConfirmationLetter(EraldyApiApp appApi, HashMap<String,Object> data) {

    htmlTemplate = appApi.getTemplate(TEMPLATE_NAME);
    this.data = data;

  }

  public static Config config(EraldyApiApp appApi) {
    return new Config(appApi);
  }

  public String getHtml() {

    htmlTemplate.applyVariables(data);
    return htmlTemplate.getResult();

  }


  public static class Config {


    private final EraldyApiApp apiApp;
    HashMap<String, Object> hashMap = new HashMap<>();

    public Config(EraldyApiApp apiApp) {
      this.apiApp = apiApp;
    }


    public Config withRoutingContext(RoutingContext routingContext) {
      Boolean debug = routingContext.request().params().contains(URI_DEBUG_PARAMETER);
      hashMap.put(URI_DEBUG_PARAMETER, debug);
      return this;
    }

    public Config setPublisherName(String publisherName) {
      hashMap.put("publisherName", publisherName);
      return this;
    }

    public Config setPublisherEmail(String publisherEmail) {
      hashMap.put("publisherEmail", publisherEmail);
      return this;
    }

    public Config setPublisherLogo(URI uri) {
      hashMap.put("publisherLogoSrc", uri);
      return this;
    }

    public Config setPublicationName(String publicationName) {
      hashMap.put("publicationName",publicationName);
      return this;
    }

    public Config setSubscriberName(String subscriberName) {
      hashMap.put("subscriberName",subscriberName);
      return this;
    }

    public ListRegistrationConfirmationLetter build() {
      return new ListRegistrationConfirmationLetter(apiApp, hashMap);
    }

    public Config addMapData(Map<String, Object> map) {
      hashMap.putAll(map);
      return this;
    }

    public Config setPublisherTitle(String title) {
      hashMap.put("publisherTitle",title);
      return this;
    }

    public Config setPublisherAvatar(URI avatar) {
      hashMap.put("publisherAvatar",avatar);
      return this;
    }

    public Config setPublisherFullname(String fullname) {
      hashMap.put("publisherFullname",fullname);
      return this;
    }

  }
}
