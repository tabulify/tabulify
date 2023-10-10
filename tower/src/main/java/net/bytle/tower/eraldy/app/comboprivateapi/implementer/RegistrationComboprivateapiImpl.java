package net.bytle.tower.eraldy.app.comboprivateapi.implementer;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.codec.impl.BodyCodecImpl;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.app.comboprivateapi.implementer.letter.ListRegistrationConfirmationLetter;
import net.bytle.tower.eraldy.app.comboprivateapi.implementer.letter.ListRegistrationValidationLetter;
import net.bytle.tower.eraldy.app.comboprivateapi.implementer.model.ListRegistrationValidationToken;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces.RegistrationComboprivateapi;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.UsersUtil;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.ListProvider;
import net.bytle.tower.eraldy.objectProvider.ListRegistrationProvider;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.tower.util.FailureStatic;
import net.bytle.tower.util.HttpRequestUtil;
import net.bytle.tower.util.JsonToken;
import net.bytle.tower.util.TemplateEngine;
import net.bytle.type.Casts;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.MailServiceSmtpProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class RegistrationComboprivateapiImpl implements RegistrationComboprivateapi {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationComboprivateapiImpl.class);
  public static final String REGISTRATION_EMAIL_SUBJECT_PREFIX = "User Registration: ";

  @Override
  public Future<ApiResponse<Registration>> listRegistrationGet(RoutingContext routingContext, String guid, String
    listGuid, String subscriberEmail) {

    Vertx vertx = routingContext.vertx();
    Future<Registration> futureRegistration;
    ListRegistrationProvider registrationProvider = ListRegistrationProvider.create(vertx);
    if (guid != null) {
      futureRegistration = registrationProvider
        .getRegistrationByGuid(guid);
    } else if (listGuid != null || subscriberEmail != null) {
      if (listGuid == null) {
        throw IllegalArgumentExceptions.createWithInputNameAndValue("The listGuid should be given with a subscriberEmail", "listGuid", null);
      }
      if (subscriberEmail == null) {
        throw IllegalArgumentExceptions.createWithInputNameAndValue("The subscriberEmail should be given with a listGuid", "subscriberEmail", null);
      }
      futureRegistration = registrationProvider
        .getRegistrationByListGuidAndSubscriberEmail(listGuid, subscriberEmail);
    } else {
      throw IllegalArgumentExceptions.createWithInputNameAndValue("A registration guid or a listGuid and a subscriberEmail should be given", "guid", null);
    }
    return futureRegistration
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(subscription -> {
        Registration subscriptionClone = registrationProvider.toPublicClone(subscription);
        /**
         * The realm is deleted by default
         */
        subscriptionClone.getList().setRealm(RealmProvider.createFrom(vertx).toPublicClone(subscription.getList().getRealm()));
        return Future.succeededFuture(new ApiResponse<>(subscription));
      });

  }


  @Override
  public Future<ApiResponse<String>> listRegistrationValidationGet(RoutingContext routingContext, String data) {


    Vertx vertx = routingContext.vertx();
    JsonObject jsonData = JsonToken.get(vertx)
      .decrypt(data, ListRegistrationValidationLetter.REGISTRATION_VALIDATION_CIPHER);

    ListRegistrationValidationToken token = BodyCodecImpl.jsonDecoder(ListRegistrationValidationToken.class)
      .apply(jsonData.toBuffer());

    /**
     * Processing
     */
    String listGuid = token.getPublicationGuid();
    return ListProvider
      .create(vertx)
      .getListByGuid(listGuid)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(listResult -> {

        User registrationUser = new User();
        registrationUser.setRealm(listResult.getRealm());
        registrationUser.setName(token.getUserName());
        registrationUser.setEmail(token.getUserEmail());
        Future<RegistrationList> listFuture = Future.succeededFuture(listResult);
        Future<User> user = UserProvider
          .createFrom(vertx)
          .getOrCreateUserFromEmail(registrationUser);

        return Future.all(listFuture, user);
      })
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(compositeFuture -> {
        RegistrationList registrationList = compositeFuture.resultAt(0);
        User subscriber = compositeFuture.resultAt(1);
        /**
         * The token is validated, we can create the subscription
         */
        Registration listRegistration = new Registration();
        listRegistration.setFlow(RegistrationFlow.EMAIL);
        listRegistration.setList(registrationList);
        listRegistration.setSubscriber(subscriber);
        listRegistration.setOptInTime(token.getOptInTime());
        listRegistration.setOptInIp(token.getOptInIp());
        listRegistration.setConfirmationTime(Timestamp.createFromNow().toIsoString());
        try {
          String realRemoteClient = HttpRequestUtil.getRealRemoteClientIp(routingContext.request());
          listRegistration.setConfirmationIp(realRemoteClient);
        } catch (NotFoundException e) {
          LOGGER.warn("Publication Subscription Validation: The remote ip client could not be found. Error: " + e.getMessage());
        }

        return ListRegistrationProvider.create(vertx)
          .upsertRegistration(listRegistration);
      })
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(registrationResult -> {

        // Send feedback to the list owner
        String title = "The user (" + registrationResult.getSubscriber().getEmail() + ") validated its subscription to the list (" + registrationResult.getList().getHandle() + ").";
        User listOwnerUser = ListProvider.getOwnerUser(registrationResult.getList());
        String listOwnerEmailRfc = UsersUtil.getEmailAddressWithName(listOwnerUser);
        MailServiceSmtpProvider mailServiceSmtpProvider = MailServiceSmtpProvider.get(vertx);
        MailMessage ownerFeedbackEmail = mailServiceSmtpProvider
          .createVertxMailMessage()
          .setTo(listOwnerEmailRfc)
          .setFrom(listOwnerEmailRfc)
          .setSubject(REGISTRATION_EMAIL_SUBJECT_PREFIX + title)
          .setText(title)
          .setHtml("<html><body>" + title + "</body></html>");
        mailServiceSmtpProvider
          .getVertxMailClientForSenderWithSigning(listOwnerUser.getEmail())
          .sendMail(ownerFeedbackEmail)
          .onFailure(t -> LOGGER.error("Error while sending the list owner registration feedback email", t));

        /**
         * Confirmation Letter (ie HTML page)
         */
        ListRegistrationConfirmationLetter.Config letter = ListRegistrationConfirmationLetter
          .config()
          .withRoutingContext(routingContext)
          .addMapData(jsonData.getMap());

        RegistrationList registrationList = registrationResult.getList();
        letter.setPublicationName(registrationList.getName());
        // Subscriber
        User subscriberUser = registrationResult.getSubscriber();
        letter.setSubscriberName(subscriberUser.getName());
        // Publisher
        User publisherUser = ListProvider.getOwnerUser(registrationList);
        letter
          .setPublisherName(publisherUser.getName())
          .setPublisherFullname(publisherUser.getFullname())
          .setPublisherEmail(publisherUser.getEmail())
          .setPublisherTitle(publisherUser.getTitle())
          .setPublisherAvatar(publisherUser.getAvatar())
        ;
        App publisherApp = registrationList.getOwnerApp();
        // https://combostrap.com/_media/android-chrome-192x192.png"
        URI logo = publisherApp.getLogo();
        if (logo != null) {
          letter.setPublisherLogo(logo);
        }

        String html = letter
          .build()
          .getHtml();


        /**
         * Response
         */
        HttpServerResponse response = routingContext.response();
        response.putHeader("Content-Type", "text/html");
        response
          .setStatusCode(200)
          .end(html);

        return Future.succeededFuture(new ApiResponse<>());
      });

  }

  @Override
  public Future<ApiResponse<java.util.List<RegistrationShort>>> listRegistrationsGet(RoutingContext
                                                                                       routingContext, String publicationGuid) {
    Vertx vertx = routingContext.vertx();
    return ListRegistrationProvider.create(vertx)
      .getRegistrations(publicationGuid)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(subscriptionShorts -> Future.succeededFuture(new ApiResponse<>(subscriptionShorts)));
  }


  @Override
  public Future<ApiResponse<String>> listRegistrationLetterConfirmationGet(RoutingContext routingContext, String
    subscriberName, String publicationGuid, String publicationName, String publisherName, String
                                                                             publisherEmail, String
                                                                             publisherLogo) {


    MultiMap params = routingContext.request().params();
    Map<String, String> singleParams = HttpRequestUtil.paramsToMap(params);
    String html;
    try {
      html = ListRegistrationConfirmationLetter
        .config()
        .addMapData(Casts.castToSameMap(singleParams, String.class, Object.class))
        .build()
        .getHtml();
    } catch (CastException e) {
      throw new InternalException("A cast on string and object should not throw");
    }

    /**
     * The open api generated code manage for now only json data
     * See {@link ListApiHandler#publicationSubscriptionValidationLetterGet(RoutingContext)}
     * We send therefore the data before
     * and the response is empty
     */
    routingContext.response().putHeader("Content-Type", "text/html");
    routingContext.response().send(html);

    return Future.succeededFuture(new ApiResponse<>());
  }

  @Override
  public Future<ApiResponse<String>> listRegistrationLetterValidationGet(RoutingContext routingContext, String
    listGuid, String subscriberName, String subscriberEmail, Boolean debug) {

    ListRegistrationPostBody listRegistrationPostBody = new ListRegistrationPostBody();
    listRegistrationPostBody.setSubscriberEmail(subscriberEmail);
    listRegistrationPostBody.setListGuid(listGuid);

    Vertx vertx = routingContext.vertx();
    return ListProvider.create(vertx)
      .getListByGuid(listGuid)
      .compose(list -> {

        if (list == null) {
          Map<String, Object> variables = new HashMap<>();
          variables.put("title", "The list guid was not found");
          String message = "The list guid (" + listGuid + ") was not found";
          variables.put("message", message);
          String errorHtml = TemplateEngine.getLocalHtmlEngine(vertx)
            .compile("Error.html")
            .applyVariables(variables)
            .getResult();
          routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code());
          routingContext.response().putHeader("Content-Type", "text/html");
          routingContext.response().send(errorHtml);
          return Future.succeededFuture(new ApiResponse<>(HttpResponseStatus.NOT_FOUND.code()));
        }

        String html = ListRegistrationValidationLetter
          .config(routingContext.vertx())
          .withRoutingContext(routingContext)
          .withSubscriptionPostObject(listRegistrationPostBody)
          .withRegistrationList(list)
          .build()
          .getEmailHTML();

        /**
         * The open api generated code manage for now only json data
         * See {@link ListApiHandler#publicationSubscriptionValidationLetterGet(RoutingContext)}
         * We send therefore the data before
         * and the response is empty
         */
        routingContext.response().putHeader("Content-Type", "text/html");
        routingContext.response().send(html);

        return Future.succeededFuture(new ApiResponse<>());
      });

  }

}
