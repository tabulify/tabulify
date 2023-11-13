package net.bytle.tower.eraldy.api.implementer;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.codec.impl.BodyCodecImpl;
import io.vertx.json.schema.ValidationException;
import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.letter.ListRegistrationConfirmationLetter;
import net.bytle.tower.eraldy.api.implementer.letter.ListRegistrationValidationLetter;
import net.bytle.tower.eraldy.api.implementer.model.ListRegistrationValidationToken;
import net.bytle.tower.eraldy.api.openapi.interfaces.ListApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.ListProvider;
import net.bytle.tower.eraldy.objectProvider.ListRegistrationProvider;
import net.bytle.type.Casts;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class ListApiImpl implements ListApi {

  private static final Logger LOGGER = LoggerFactory.getLogger(ListApiImpl.class);
  private final EraldyApiApp apiApp;

  public ListApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
  }

  @Override
  public Future<ApiResponse<List<RegistrationList>>> listsGet(RoutingContext routingContext, String appGuid, String appUri, String realmIdentifier) {

    if (appGuid == null && appUri == null && realmIdentifier == null) {
      throw ValidationException.create("A app or realm definition should be given to get a list of lists.", "any", null);
    }

    Future<List<RegistrationList>> futureLists;
    ListProvider listProvider = apiApp.getListProvider();
    if (appGuid == null && appUri == null) {

      /**
       * Realms selection
       */
      futureLists = this.apiApp.getRealmProvider()
        .getRealmFromIdentifier(realmIdentifier, Realm.class)
        .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
        .compose(listProvider::getListsForRealm);

    } else {

      /**
       * App selections
       */
      Future<App> futureApp;
      if (appGuid != null) {
        futureApp = apiApp.getAppProvider()
          .getAppByGuid(appGuid);
      } else {
        if (realmIdentifier == null) {
          throw ValidationException.create("The realm identifier (guid or handle) should be given for an appUri", "realmIdentifier", null);
        }
        futureApp = this.apiApp.getRealmProvider()
          .getRealmFromIdentifier(realmIdentifier)
          .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
          .compose(realm -> apiApp.getAppProvider().getAppByUri(URI.create(appUri), realm));
      }

      futureLists = futureApp
        .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
        .compose(app -> {
          if (app == null) {
            NoSuchElementException noSuchElementException = new NoSuchElementException("The app could not be found");
            return Future.failedFuture(noSuchElementException);
          }
          return listProvider.getListsForApp(app);
        });

    }

    return futureLists
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(lists -> {
        List<RegistrationList> publicRegistrationLists = lists
          .stream().map(listProvider::toPublicClone)
          .collect(Collectors.toList());
        ApiResponse<java.util.List<RegistrationList>> apiResponse = new ApiResponse<>(publicRegistrationLists);
        return Future.succeededFuture(apiResponse);
      });

  }


  @Override
  public Future<ApiResponse<java.util.List<ListSummary>>> listsSummaryGet(RoutingContext routingContext, String realmIdentifier) {

    if (realmIdentifier == null) {
      throw ValidationException.create("The realm identifier should be given", "realmIdentifier", null);
    }

    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifier(realmIdentifier)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(realm -> {
        if (realm == null) {
          throw new InternalException("The realm was not found and is mandatory");
        }
        return apiApp.getListProvider()
          .getListsSummary(realm);
      })
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(subscribersSummary -> Future.succeededFuture(new ApiResponse<>(subscribersSummary)));
  }


  @Override
  public Future<ApiResponse<RegistrationList>> listGet(RoutingContext routingContext, String listGuid, String listHandle, String realmHandle) {

    Future<RegistrationList> listFuture;
    ListProvider listProvider = apiApp.getListProvider();
    if (listGuid != null) {
      listFuture = listProvider.getListByGuid(listGuid);
    } else {
      if (listHandle == null) {
        throw ValidationException.create("A listGuid or a listHandle is mandatory to retrieve a list", "listGuid", null);
      }
      if (realmHandle == null) {
        throw ValidationException.create("A realm Handle is mandatory to retrieve a list with a listHandle", "realmHandle", null);
      }
      listFuture = this.apiApp.getRealmProvider()
        .getRealmFromHandle(realmHandle)
        .compose(realm -> listProvider.getListByHandle(listHandle, realm));
    }
    return listFuture
      .compose(registrationList -> {
        RegistrationList registrationListClone = listProvider.toPublicClone(registrationList);
        /**
         * The realm is deleted by default, but we need it on the frontend
         */
        registrationListClone.setRealm(this.apiApp.getRealmProvider().toPublicClone(registrationList.getRealm()));
        ApiResponse<RegistrationList> apiResult = new ApiResponse<>(registrationListClone);
        return Future.succeededFuture(apiResult);
      });
  }

  @Override
  public Future<ApiResponse<RegistrationList>> listPost(RoutingContext routingContext, ListPostBody publicationPost) {

    return apiApp.getListProvider()
      .postPublication(publicationPost)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(publication -> {
        apiApp.getListProvider().toPublicClone(publication);
        return Future.succeededFuture(new ApiResponse<>(publication));
      });


  }


  @Override
  public Future<ApiResponse<String>> listRegisterConfirmationRegistrationGet(RoutingContext routingContext, String registrationGuid, String redirectUri) {
    /**
     * Note that the redirection uri is not mandatory
     * and is used by the front end to redirect if present
     */
    return this.apiApp.getUserListRegistrationFlow().handleStep3Confirmation(routingContext, registrationGuid);
  }

  public static final String REGISTRATION_EMAIL_SUBJECT_PREFIX = "User Registration: ";

  @Override
  public Future<ApiResponse<Registration>> listRegistrationGet(RoutingContext routingContext, String guid, String
    listGuid, String subscriberEmail) {

    Future<Registration> futureRegistration;
    ListRegistrationProvider registrationProvider = apiApp.getListRegistrationProvider();
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
        subscriptionClone.getList().setRealm(this.apiApp.getRealmProvider().toPublicClone(subscription.getList().getRealm()));
        return Future.succeededFuture(new ApiResponse<>(subscription));
      });

  }


  @Override
  public Future<ApiResponse<String>> listRegistrationValidationGet(RoutingContext routingContext, String data) {


    Vertx vertx = routingContext.vertx();
    JsonToken jsonToken = this.apiApp.getApexDomain().getHttpServer().getServer().getJsonToken();
    JsonObject jsonData = jsonToken.decrypt(data, ListRegistrationValidationLetter.REGISTRATION_VALIDATION_CIPHER);

    ListRegistrationValidationToken token = BodyCodecImpl.jsonDecoder(ListRegistrationValidationToken.class)
      .apply(jsonData.toBuffer());

    /**
     * Processing
     */
    String listGuid = token.getPublicationGuid();
    return apiApp.getListProvider()
      .getListByGuid(listGuid)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(listResult -> {

        User registrationUser = new User();
        registrationUser.setRealm(listResult.getRealm());
        registrationUser.setName(token.getUserName());
        registrationUser.setEmail(token.getUserEmail());
        Future<RegistrationList> listFuture = Future.succeededFuture(listResult);
        Future<User> user = apiApp.getUserProvider()
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

        return apiApp.getListRegistrationProvider()
          .upsertRegistration(listRegistration);
      })
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(registrationResult -> {

        // Send feedback to the list owner
        String title = "The user (" + registrationResult.getSubscriber().getEmail() + ") validated its subscription to the list (" + registrationResult.getList().getHandle() + ").";
        User listOwnerUser = ListProvider.getOwnerUser(registrationResult.getList());
        String listOwnerEmailRfc;
        try {
          listOwnerEmailRfc = BMailInternetAddress.of(listOwnerUser.getEmail(), listOwnerUser.getName()).toString();
        } catch (AddressException e) {
          throw new InternalException("The list owner email is not valid", e);
        }
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
         * Confirmation Letter (c.-à-d. HTML page)
         */
        ListRegistrationConfirmationLetter.Config letter = ListRegistrationConfirmationLetter
          .config(this.apiApp)
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
  public Future<ApiResponse<List<RegistrationShort>>> listRegistrationsGet(RoutingContext routingContext, String publicationGuid) {
    return apiApp.getListRegistrationProvider()
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
        .config(this.apiApp)
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
    return apiApp.getListProvider()
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
          .config(this.apiApp)
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
