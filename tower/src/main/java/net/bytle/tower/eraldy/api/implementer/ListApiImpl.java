package net.bytle.tower.eraldy.api.implementer;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.exception.NullValueException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.implementer.flow.ListImportFlow;
import net.bytle.tower.eraldy.api.implementer.flow.ListImportJob;
import net.bytle.tower.eraldy.api.implementer.flow.ListImportListUserAction;
import net.bytle.tower.eraldy.api.implementer.flow.ListImportUserAction;
import net.bytle.tower.eraldy.api.implementer.letter.ListRegistrationConfirmationLetter;
import net.bytle.tower.eraldy.api.implementer.letter.ListRegistrationValidationLetter;
import net.bytle.tower.eraldy.api.openapi.interfaces.ListApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthScope;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.ListProvider;
import net.bytle.tower.eraldy.objectProvider.ListUserProvider;
import net.bytle.tower.util.Guid;
import net.bytle.type.Casts;
import net.bytle.type.Strings;
import net.bytle.vertx.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ListApiImpl implements ListApi {

  private final EraldyApiApp apiApp;

  public ListApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
  }

  @Override
  public Future<ApiResponse<List<ListItem>>> listsGet(RoutingContext routingContext, String appGuid, String appUri, String realmIdentifier) {

    if (appGuid == null && appUri == null && realmIdentifier == null) {
      throw ValidationException.create("A app or realm definition should be given to get a list of lists.", "any", null);
    }

    Future<List<ListItem>> futureLists;
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
          .compose(realm -> apiApp.getAppProvider().getAppByHandle(appUri, realm));
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
        ApiResponse<java.util.List<ListItem>> apiResponse = new ApiResponse<>(lists)
          .setMapper(listProvider.getApiMapper());
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
  public Future<ApiResponse<ListImportPostResponse>> listListImportPost(RoutingContext routingContext, String listIdentifier, Integer rowCountToProcess, FileUpload fileBinary) {

    RoutingContextWrapper routingContextWrapper = new RoutingContextWrapper(routingContext);
    rowCountToProcess = routingContextWrapper.getRequestQueryParameterAsInteger("rowCountToProcess", 10000);
    if (rowCountToProcess < 0) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("The rowCountToProcess parameters value (" + rowCountToProcess + ") should not be negative")
          .buildWithContextFailing(routingContext)
      );
    }
    if (rowCountToProcess == 0) {
      return Future.failedFuture(
        TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
          .setMessage("The rowCountToProcess parameters value (" + rowCountToProcess + ") is zero")
          .buildWithContextFailing(routingContext)
      );
    }

    Integer finalRowCountToProcess = rowCountToProcess;
    return this.apiApp.getListProvider()
      .getListByGuidHashIdentifier(listIdentifier)
      .compose(list -> {
        if (list == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The list guid (" + listIdentifier + ") was not found")
              .buildWithContextFailing(routingContext)
          );
        }
        return this.apiApp.getAuthProvider().checkListAuthorization(routingContext, list, AuthScope.LIST_IMPORT);
      })
      .compose(list -> {
        ListImportJob importJob = this.apiApp.getListImportFlow()
          .buildJob(list, fileBinary, ListImportListUserAction.IN)
          .setMaxRowCountToProcess(finalRowCountToProcess)
          .setUserAction(ListImportUserAction.UPDATE)
          .build();
        String jobId;
        try {
          jobId = this.apiApp.getListImportFlow().step1AddJobToQueue(importJob);
        } catch (TowerFailureException e) {
          return Future.failedFuture(e);
        }
        ListImportPostResponse listListImportPost200Response = new ListImportPostResponse();
        listListImportPost200Response.setJobIdentifier(jobId);
        return Future.succeededFuture(new ApiResponse<>(listListImportPost200Response));
      });


  }

  @Override
  public Future<ApiResponse<List<ListImportJobStatus>>> listListImportsGet(RoutingContext routingContext, String listIdentifier) {
    List<ListImportJobStatus> listImportJobs = this.apiApp
      .getListImportFlow()
      .getJobsStatuses(listIdentifier);
    return Future.succeededFuture(new ApiResponse<>(listImportJobs));
  }

  @Override
  public Future<ApiResponse<Void>> listListDelete(RoutingContext routingContext, String listIdentifier, String realmIdentifier) {

    ListProvider listProvider = this.apiApp.getListProvider();
    return listProvider
      .getListByIdentifier(routingContext, AuthScope.LIST_DELETE, ListItem.class)
      .compose(listItem -> {

        if (listItem == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404)
            .setMessage("The list (id:" + listIdentifier + ", realm:" + Objects.requireNonNullElse(realmIdentifier, "") + ") was not found")
            .build()
          );
        }
        return listProvider
          .deleteByList(listItem)
          .compose(res -> Future.succeededFuture(new ApiResponse<>()));

      });
  }


  @Override
  public Future<ApiResponse<ListItemAnalytics>> listListGet(RoutingContext routingContext, String listIdentifier, String realmIdentifier) {
    ListProvider listProvider = this.apiApp.getListProvider();
    return listProvider
      .getListByIdentifier(routingContext, AuthScope.LIST_GET, ListItemAnalytics.class)
      .compose(listItemAnalytics -> {
        ApiResponse<ListItemAnalytics> apiResult = new ApiResponse<>(listItemAnalytics)
          .setMapper(listProvider.getApiMapper());
        return Future.succeededFuture(apiResult);
      });
  }

  @Override
  public Future<ApiResponse<List<ListImportJobRowStatus>>> listListImportJobDetailsGet(RoutingContext routingContext, String listIdentifier, String jobIdentifier) {

    Path path = this.apiApp
      .getListImportFlow()
      .getRowStatusFileJobByIdentifier(listIdentifier, jobIdentifier);

    routingContext
      .response()
      .sendFile(path.toAbsolutePath().toString());

    return Future.succeededFuture();

  }

  @Override
  public Future<ApiResponse<ListImportJobStatus>> listListImportJobGet(RoutingContext routingContext, String listIdentifier, String jobIdentifier) {

    ListImportFlow listImportFlow = this.apiApp.getListImportFlow();

    // Running, Queued Job
    try {
      ListImportJobStatus listImportStatus = listImportFlow.getQueuedJob(jobIdentifier)
        .getStatus();
      return Future.succeededFuture(new ApiResponse<>(listImportStatus));
    } catch (NullValueException e) {
      // not found
    }

    // Processed Job On file system?
    Path path = listImportFlow
      .getStatusFileJobByIdentifier(listIdentifier, jobIdentifier);
    if (!Files.exists(path)) {
      return Future.failedFuture(TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.NOT_FOUND_404)
        .setMessage("The job (" + jobIdentifier + ") for the list (" + listIdentifier + ") was not found")
        .build()
      );
    }
    String string = Strings.createFromPath(path).toString();
    ListImportJobStatus listImportStatus = new JsonObject(string).mapTo(ListImportJobStatus.class);
    return Future.succeededFuture(new ApiResponse<>(listImportStatus));
  }


  @Override
  public Future<ApiResponse<ListItem>> listListPatch(RoutingContext routingContext, String listIdentifier, ListBody listBody, String realmIdentifier) {
    ListProvider listProvider = this.apiApp.getListProvider();
    return listProvider.getListByIdentifier(routingContext, AuthScope.LIST_PATCH, ListItem.class)
      .compose(list -> {
        String listHandle = listBody.getListHandle();
        if (listHandle != null) {
          list.setHandle(listHandle);
        }
        String listName = listBody.getListName();
        if (listName != null) {
          list.setName(listName);
        }
        String listTitle = listBody.getListTitle();
        if (listTitle != null) {
          list.setTitle(listTitle);
        }
        String listDescription = listBody.getListDescription();
        if (listDescription != null) {
          list.setDescription(listDescription);
        }
        String ownerIdentifier = listBody.getOwnerUserIdentifier();
        Future<User> futureOwner;
        User actualOwnerUser = list.getOwnerUser();
        if (ownerIdentifier != null) {
          if (ownerIdentifier.isEmpty()) {
            futureOwner = Future.succeededFuture(null);
          } else {
            Future<User> futureOwnerByIdentifier = this.apiApp.getUserProvider().getUserByIdentifier(ownerIdentifier, list.getRealm(), User.class);
            if (actualOwnerUser == null) {
              futureOwner = futureOwnerByIdentifier;
            } else if (!(actualOwnerUser.getGuid().equals(ownerIdentifier) || actualOwnerUser.getEmail().equals(ownerIdentifier))) {
              futureOwner = futureOwnerByIdentifier;
            } else {
              futureOwner = Future.succeededFuture(actualOwnerUser);
            }
          }
        } else {
          futureOwner = Future.succeededFuture(actualOwnerUser);
        }
        return futureOwner
          .compose(newOwner -> {
            list.setOwnerUser(newOwner);
            return listProvider
              .updateList(list)
              .compose(updatedList -> Future.succeededFuture(new ApiResponse<>(updatedList).setMapper(listProvider.getApiMapper())));
          });

      });

  }


  @Override
  public Future<ApiResponse<String>> listUserConfirmationUserGet(RoutingContext routingContext, String registrationGuid, String redirectUri) {
    /**
     * Note that the redirection uri is not mandatory
     * and is used by the front end to redirect if present
     */
    return this.apiApp.getUserListRegistrationFlow().handleStep3Confirmation(routingContext, registrationGuid);
  }

  public static final String REGISTRATION_EMAIL_SUBJECT_PREFIX = "User Registration: ";

  @Override
  public Future<ApiResponse<ListUser>> listUserIdentifierGet(RoutingContext routingContext, String guid) {


    ListUserProvider listUserProvider = apiApp.getListRegistrationProvider();

    return listUserProvider
      .getListUserByGuidHash(guid)
      .compose(listUser -> Future.succeededFuture(new ApiResponse<>(listUser).setMapper(listUserProvider.getApiMapper())));

  }

  @Override
  public Future<ApiResponse<List<ListUserShort>>> listListUsersGet(RoutingContext routingContext, String listIdentifier, Long pageId, Long pageSize, String searchTerm) {
    RoutingContextWrapper routingContextWrapper = RoutingContextWrapper.createFrom(routingContext);
    try {
      listIdentifier = routingContextWrapper.getRequestPathParameter("listIdentifier").getString();
    } catch (NotFoundException e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .setMessage("The list identifier is mandatory on the request path and was not found")
        .build()
      );
    }
    pageId = routingContextWrapper.getRequestQueryParameterAsLong("pageId", 0L);
    pageSize = routingContextWrapper.getRequestQueryParameterAsLong("pageSize", 10L);
    searchTerm = routingContextWrapper.getRequestQueryParameterAsString("searchTerm", null);
    Guid guid;
    try {
      guid = apiApp.getListProvider().getGuidObject(listIdentifier);
    } catch (CastException e) {
      return Future.failedFuture(TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.BAD_REQUEST_400)
        .setMessage("The list identifier (" + listIdentifier + ") is not a guid")
        .build()
      );
    }
    String finalListIdentifier = listIdentifier;
    Long finalPageId = pageId;
    Long finalPageSize = pageSize;
    String finalSearchTerm = searchTerm;
    long realmId = guid.getRealmOrOrganizationId();
    return this.apiApp
      .getAuthProvider()
      .checkRealmAuthorization(routingContext, realmId, AuthScope.LIST_GET_REGISTRATIONS)
      .compose(realmIdRes -> apiApp.getListRegistrationProvider()
        .getListUsers(finalListIdentifier, finalPageId, finalPageSize, finalSearchTerm)
        .compose(subscriptionShorts -> Future.succeededFuture(new ApiResponse<>(subscriptionShorts))));

  }


  @Override
  public Future<ApiResponse<String>> listUserLetterConfirmationGet(RoutingContext routingContext, String
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
  public Future<ApiResponse<String>> listUserLetterValidationGet(RoutingContext routingContext, String
    listGuid, String subscriberName, String subscriberEmail, Boolean debug) {

    ListUserPostBody listRegistrationPostBody = new ListUserPostBody();
    listRegistrationPostBody.setUserEmail(subscriberEmail);
    listRegistrationPostBody.setListGuid(listGuid);

    Vertx vertx = routingContext.vertx();
    return apiApp
      .getListProvider()
      .getListByGuidHashIdentifier(listGuid)
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
