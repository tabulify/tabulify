package net.bytle.tower.eraldy.app.comboprivateapi.implementer.letter;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.email.BMailTransactionalTemplate;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.app.comboprivateapi.ComboPrivateApiApp;
import net.bytle.tower.eraldy.app.comboprivateapi.implementer.model.ListRegistrationValidationToken;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.ListRegistrationPostBody;
import net.bytle.tower.eraldy.model.openapi.RegistrationList;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.util.HttpRequestUtil;
import net.bytle.tower.util.JsonToken;
import net.bytle.tower.util.JsonTokenCipher;
import net.bytle.tower.util.TemplateEngine;
import net.bytle.type.Booleans;
import net.bytle.type.UriEnhanced;

import javax.mail.internet.AddressException;

/**
 * The letter (in HTML format)
 * that is sent by email to validate a registration
 * by clicking on the validation link
 */
public class ListRegistrationValidationLetter {


  public static final String URI_PATH = "/list/registration/validation";
  public static final String URI_DATA_PARAMETER = "data";
  /**
   * If the Get link has a debug parameter, all template variables/data are printed
   */
  private static final String URI_DEBUG_PARAMETER = "debug";
  public static final JsonTokenCipher REGISTRATION_VALIDATION_CIPHER = JsonTokenCipher.BUFFER_BYTES;
  private final BMailTransactionalTemplate template;

  public ListRegistrationValidationLetter(BMailTransactionalTemplate template) {
    this.template = template;
  }

  public static Config config(Vertx vertx) {
    return new Config(vertx);
  }

  public static UriEnhanced getValidationUri(RoutingContext routingContext, ListRegistrationValidationToken validationToken) {
    String encryptedData = getEncryptedData(validationToken, routingContext.vertx());
    return ComboPrivateApiApp
      .get()
      .getPublicRequestUriForOperationPath(URI_PATH)
      .addQueryProperty(URI_DATA_PARAMETER, encryptedData);
  }

  public static String getEncryptedData(ListRegistrationValidationToken validationToken, Vertx vertx) {
    JsonObject validationJson = JsonObject.mapFrom(validationToken);
    return JsonToken.get(vertx)
      .encrypt(validationJson, REGISTRATION_VALIDATION_CIPHER);
  }


  public String getEmailHTML() {
    return template.generateHTMLForEmail();
  }

  public String getPlainText() {
    return template.generatePlainText();
  }

  public static class Config {


    private final BMailTransactionalTemplate transactionalTemplate;
    private RoutingContext routingContext;
    private ListRegistrationPostBody subscriptionPostObject;
    private RegistrationList registrationList;

    public Config(Vertx vertx) {
      net.bytle.template.api.TemplateEngine towerEngine = TemplateEngine.getEmailEngine(vertx);
      transactionalTemplate = BMailTransactionalTemplate
        .createFromName(BMailTransactionalTemplate.DEFAULT_TEMPLATE_NAME, towerEngine);
    }

    public Config withRoutingContext(RoutingContext routingContext) {
      this.routingContext = routingContext;
      MultiMap params = routingContext.request().params();
      if (params.contains(URI_DEBUG_PARAMETER)) {
        String debugValue = params.get(URI_DEBUG_PARAMETER);
        Boolean debug;
        if (debugValue != null) {
          debug = Booleans.createFromString(debugValue).toBoolean();
        } else {
          debug = true;
        }
        transactionalTemplate.setDebug(debug);
      }
      return this;
    }

    public ListRegistrationValidationLetter build() {
      /**
       * We use not an openapi endpoint for the following reason:
       * - we make sure that the path exists and was not changed in case of refactoring
       * - we may send back a html page (openapi does not allow to send back html for the moment)
       */
      ListRegistrationValidationToken publicationSubscriptionConfirmationToken = ListRegistrationValidationToken
        .config()
        .addOptInContext(routingContext)
        .setFromListObject(subscriptionPostObject)
        .build();
      UriEnhanced validationUri = getValidationUri(routingContext, publicationSubscriptionConfirmationToken);
      if (HttpRequestUtil.isLocalhostRequest(routingContext)) {
        // only in a test environment, to not modify the host file when testing with an external http client such as postman
        validationUri.addQueryProperty(RealmProvider.REALM_HANDLE_URL_PARAMETER, registrationList.getRealm().getHandle());
      }
      String validationUrl = validationUri.toUri().toString();


      String recipientName = null;
      User subscriber = new User();
      subscriber.setEmail(subscriptionPostObject.getSubscriberEmail());
      try {
        recipientName = UsersUtil.getNameOrNameFromEmail(subscriber);
      } catch (NotFoundException e) {
        // ok
      } catch (AddressException e) {
        throw ValidationException.create("The provided email is not valid", "userEmail", subscriber.getEmail());
      }

      String publisherName;
      User publisher = registrationList.getOwnerUser();
      if (publisher == null) {
        publisher = registrationList.getOwnerApp().getUser();
      }

      try {
        publisherName = UsersUtil.getNameOrNameFromEmail(publisher);
      } catch (NotFoundException | AddressException e) {
        // should not happen
        throw new InternalException(e);
      }

      App publisherApp = registrationList.getOwnerApp();

      transactionalTemplate
        .setPreview("Validate your subscription to `" + registrationList.getName() + "`")
        .setSalutation("Hy")
        .setRecipientName(recipientName)
        .addIntroParagraph(
          "I just got a subscription request to the <mark>" + registrationList.getName() + "</mark> publication with your email." +
            "<br>For bot and consent protections, I need to check that it was really you asking.")
        .setActionUrl(validationUrl)
        .setActionName("Click on this link to validate your subscription.")
        .setActionDescription("Click on this link to validate your subscription.")
        .setActionIsGo(true)
        .addOutroParagraph(
          "I'm very excited to have you on board. " +
            "<br>Need help, or have any questions? " +
            "<br>Just reply to this email, I ❤ to help."
        )
        .setValediction("Kind Regards")
        .setSenderName(publisherName)
        .setSenderFullName(publisher.getFullname())
        .setSenderAvatar(publisher.getAvatar() != null ? publisher.getAvatar().toString() : null)
        .setSenderEmail(publisher.getEmail())
        .setSenderTitle(publisher.getTitle())
        .setBrandLogo(publisherApp.getLogo() != null ? publisherApp.getLogo().toString() : null)
        .setBrandName(publisherApp.getName())
        .setBrandUrl(publisherApp.getHome() != null ? publisherApp.getHome().toString() : null)
        .setBrandSlogan(publisherApp.getSlogan())
        .setBrandLogoWidth("25px")
        .setPrimaryColor(publisherApp.getPrimaryColor());
      return new ListRegistrationValidationLetter(transactionalTemplate);

    }

    public Config withSubscriptionPostObject(ListRegistrationPostBody publicationSubscriptionPost) {
      this.subscriptionPostObject = publicationSubscriptionPost;
      return this;
    }

    public Config withRegistrationList(RegistrationList registrationList) {
      this.registrationList = registrationList;
      return this;
    }
  }
}
