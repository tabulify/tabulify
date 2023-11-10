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

import java.net.MalformedURLException;
import java.net.URI;

public abstract class FlowCallbackAbs implements FlowCallback {

  private static final int EXPIRATION_IN_MINUTES = 5;
  protected final TowerApp app;

  /**
   * If the Get link has a debug parameter, all template variables/data are printed
   */
  private static final String URI_DEBUG_PARAMETER = "debug";
  private final JsonToken jsonToken;

  public FlowCallbackAbs(TowerApp app) {
    this.app = app;
    this.jsonToken = app.getApexDomain().getHttpServer().getServer().getJsonToken();
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
    String callbackLocalRouterPath = app.getPathMount() + this.getCallbackOperationPath();
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
   * @param routingContext  - the routing context
   * @param flowSender     - the email sender
   * @param recipientName - the name of the recipient
   * @param jwtClaimsObject - the claims (ie the user to authenticate and the state)
   * @return the email template to send for validation
   */
  public BMailTransactionalTemplate getCallbackTransactionalEmailTemplateForClaims(RoutingContext routingContext, FlowSender flowSender, String recipientName, JwtClaimsObject jwtClaimsObject) {


    JsonObject jwtClaims = jwtClaimsObject.toClaimsWithExpiration(EXPIRATION_IN_MINUTES);
    String accessToken = jsonToken.encrypt(jwtClaims, DATA_CIPHER);
    OAuthAccessTokenResponse oAuthAccessTokenResponse = new OAuthAccessTokenResponse();
    oAuthAccessTokenResponse.setAccessToken(accessToken);
    String validationUrl = getCallbackUri(oAuthAccessTokenResponse).toUri().toString();


    /**
     * Template
     */
    net.bytle.template.api.TemplateEngine templateEngine = TemplateEngine.getEmailEngine(app.getApexDomain().getHttpServer().getServer().getVertx());
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
      .setSenderName(flowSender.getName())
      .setSenderFullName(flowSender.getFullname())
      .setSenderAvatar(flowSender.getAvatar() != null ? flowSender.getAvatar().toString() : null)
      .setSenderEmail(flowSender.getEmail())
      .setSenderTitle(flowSender.getTitle())
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
  protected JwtClaimsObject getAndValidateJwtClaims(RoutingContext ctx, String linkName) throws IllegalStructure {
    OAuthAccessTokenResponse accessTokenResponse = getCallbackData(ctx, OAuthAccessTokenResponse.class);
    JsonObject jwtClaims = jsonToken.decrypt(accessTokenResponse.getAccessToken(), DATA_CIPHER);
    JwtClaimsObject jwtClaimsObject = JwtClaimsObject.createFromClaims(jwtClaims);
    try {
      jwtClaimsObject.checkValidityAndExpiration();
    } catch (IllegalStructure e) {
      ctx.fail(HttpStatus.BAD_REQUEST.httpStatusCode(), e);
      throw new IllegalStructure();
    } catch (ExpiredException e) {
      String message = "This <b>" + linkName + "</b> link has expired.";
      try {
        URI originReferer = jwtClaimsObject.getOriginReferer();
        message += " Click <a href=\"" + originReferer.toURL() + "\">here</a> to ask for a new one.";
      } catch (NullValueException | IllegalStructure | MalformedURLException ignored) {
      }
      VertxRoutingFailureData.create()
        .setDescription(message)
        .setName("Link expired")
        .failContextAsHtml(ctx);
      throw new IllegalStructure();
    }
    return jwtClaimsObject;
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

    return app
      .getOperationUriForPublicHost(this.getCallbackOperationPath())
      .addQueryProperty(URI_DATA_PARAMETER, encryptedData);

  }

  @Override
  public String getCallbackOperationPath() {
    return this.getOriginOperationPath()+"/"+ LAST_URL_OPERATION_CALLBACK_PART;
  }

  public TowerApp getApp() {
    return app;
  }
}
