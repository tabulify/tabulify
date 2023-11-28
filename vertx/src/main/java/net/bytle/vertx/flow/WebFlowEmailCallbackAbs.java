package net.bytle.vertx.flow;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import net.bytle.email.BMailTransactionalTemplate;
import net.bytle.exception.ExpiredException;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NullValueException;
import net.bytle.type.Booleans;
import net.bytle.type.UriEnhanced;
import net.bytle.vertx.*;
import net.bytle.vertx.auth.AuthUser;

import java.net.MalformedURLException;
import java.net.URI;

public abstract class WebFlowEmailCallbackAbs implements WebFlowEmailCallback {

  private static final int EXPIRATION_IN_MINUTES = 5;
  protected final WebFlow webFlow;

  /**
   * If the Get link has a debug parameter, all template variables/data are printed
   */
  private static final String URI_DEBUG_PARAMETER = "debug";
  private final JsonToken jsonToken;

  public WebFlowEmailCallbackAbs(WebFlow webFlow) {
    this.webFlow = webFlow;
    this.jsonToken = webFlow.getApp().getApexDomain().getHttpServer().getServer().getJsonToken();
  }

  /**
   * Add the callback
   * for the validation of registration
   * by clicking on the link
   * The dynamic way prevents to create a validation link
   * that does not exist
   */
  @Override
  public void addCallback(Router router) {
    String callbackLocalRouterPath = webFlow.getApp().getPathMount() + this.getCallbackOperationPath();
    router.route(callbackLocalRouterPath)
      .method(HttpMethod.GET)
      .handler(this);
  }

  @Override
  public <T> T getCallbackData(RoutingContext ctx, Class<T> clazz) {
    String dataParameter = ctx.request().getParam(URI_DATA_PARAMETER);
    return this.jsonToken
      .decrypt(dataParameter, DATA_CIPHER)
      .mapTo(clazz);
  }

  /**
   * @param routingContext - the routing context
   * @param smtpSender     - the email sender
   * @param recipientName  - the name of the recipient
   * @param authUser       - the claims (ie the user to authenticate and the state)
   * @return the email template to send for validation
   */
  public BMailTransactionalTemplate getCallbackTransactionalEmailTemplateForClaims(RoutingContext routingContext, SmtpSender smtpSender, String recipientName, AuthUser authUser) {


    JsonObject jwtClaims = authUser.toClaimsWithExpiration(EXPIRATION_IN_MINUTES);
    String accessToken = jsonToken.encrypt(jwtClaims, DATA_CIPHER);
    OAuthAccessTokenResponse oAuthAccessTokenResponse = new OAuthAccessTokenResponse();
    oAuthAccessTokenResponse.setAccessToken(accessToken);
    String validationUrl = getCallbackUri(oAuthAccessTokenResponse).toUri().toString();


    /**
     * Template
     */
    net.bytle.template.api.TemplateEngine templateEngine = TemplateEngine.getEmailEngine(webFlow.getApp().getApexDomain().getHttpServer().getServer().getVertx());
    BMailTransactionalTemplate template = BMailTransactionalTemplate
      .createFromName(BMailTransactionalTemplate.DEFAULT_TEMPLATE_NAME, templateEngine);

    /**
     * Template debug
     */
    MultiMap params = routingContext.request().params();
    if (params.contains(URI_DEBUG_PARAMETER)) {
      String debugValue = params.get(URI_DEBUG_PARAMETER);
      Boolean debug;
      if (debugValue != null) {
        debug = Booleans.createFromString(debugValue).toBoolean();
      } else {
        debug = true;
      }
      template.setDebug(debug);
    }


    /**
     * TODO: Add design for realm?
     */
    // .setBrandLogo(publisherApp.getLogo() != null ? publisherApp.getLogo().toString() : null)
    // .setBrandName(publisherApp.getName())
    // .setBrandUrl(publisherApp.getHome() != null ? publisherApp.getHome().toString() : null)
    // .setBrandSlogan(publisherApp.getSlogan())
    // .setPrimaryColor(publisherApp.getPrimaryColor());

    return template
      .setSenderName(smtpSender.getName())
      .setSenderFullName(smtpSender.getFullname())
      .setSenderAvatar(smtpSender.getAvatar() != null ? smtpSender.getAvatar().toString() : null)
      .setSenderEmail(smtpSender.getEmail())
      .setSenderTitle(smtpSender.getTitle())
      .setBrandLogoWidth("25px")
      .setActionIsGo(true)
      .setActionUrl(validationUrl)
      .setRecipientName(recipientName)
      .setSalutation("Hy")
      .setValediction("This link is valid for " + EXPIRATION_IN_MINUTES + " minutes." +
        "<br>Kind Regards");
  }

  /**
   * @param ctx      the context
   * @param linkName - the name of the link (ie login, password reset, registration, ...)
   * @return the claims
   * @throws IllegalStructure if the claims object is not valid
   */
  protected AuthUser getAndValidateJwtClaims(RoutingContext ctx, String linkName) throws IllegalStructure, TowerFailureException {
    OAuthAccessTokenResponse accessTokenResponse = getCallbackData(ctx, OAuthAccessTokenResponse.class);
    JsonObject jwtClaims = jsonToken.decrypt(accessTokenResponse.getAccessToken(), DATA_CIPHER);
    AuthUser authUser = AuthUser.createFromClaims(jwtClaims);
    try {
      authUser.checkValidityAndExpiration();
    } catch (IllegalStructure e) {
      throw TowerFailureException
        .builder()
        .setType(TowerFailureTypeEnum.BAD_CLAIMS_400)
        .setName("Bad identity claims")
        .setMessage("The identity claims are invalid")
        .setCauseException(e)
        .setMimeToHtml()
        .buildWithContextFailing(ctx);
    } catch (ExpiredException e) {
      String message = "This <b>" + linkName + "</b> link has expired.";
      try {
        URI originReferer = authUser.getOriginReferer();
        message += " Click <a href=\"" + originReferer.toURL() + "\">here</a> to ask for a new one.";
      } catch (NullValueException | IllegalStructure | MalformedURLException ignored) {
      }
      throw TowerFailureException
        .builder()
        .setType(TowerFailureTypeEnum.LINK_EXPIRED)
        .setMessage(message)
        .setName("Link expired")
        .setMimeToHtml()
        .buildWithContextFailing(ctx);
    }
    return authUser;
  }

  @Override
  public UriEnhanced getCallbackUri(Object validationObject) {

    JsonObject validationJson;
    if (validationObject instanceof JsonObject) {
      validationJson = (JsonObject) validationObject;
    } else {
      validationJson = JsonObject.mapFrom(validationObject);
    }
    String encryptedData = jsonToken.encrypt(validationJson, DATA_CIPHER);

    return webFlow
      .getApp()
      .getOperationUriForPublicHost(this.getCallbackOperationPath())
      .addQueryProperty(URI_DATA_PARAMETER, encryptedData);

  }

  @Override
  public String getCallbackOperationPath() {
    return this.getOriginOperationPath() + "/" + LAST_URL_OPERATION_CALLBACK_PART;
  }

  public WebFlow getWebFlow() {
    return webFlow;
  }

}
