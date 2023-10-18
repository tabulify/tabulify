package net.bytle.tower.eraldy.app.comboprivateapi.implementer;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.interfaces.ListComboprivateapi;
import net.bytle.tower.eraldy.app.comboprivateapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.AppProvider;
import net.bytle.tower.eraldy.objectProvider.ListProvider;
import net.bytle.tower.eraldy.objectProvider.RealmProvider;
import net.bytle.vertx.FailureStatic;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class ListComboprivateapiImpl implements ListComboprivateapi {


  @Override
  public Future<ApiResponse<List<RegistrationList>>> listsGet(RoutingContext routingContext, String appGuid, String appUri, String realmGuid, String realmHandle) {

    if (appGuid == null && appUri == null && realmGuid == null && realmHandle == null) {
      throw ValidationException.create("A app or realm definition should be given to get a list of lists.", "any", null);
    }

    Vertx vertx = routingContext.vertx();


    Future<List<RegistrationList>> futureLists;
    ListProvider listProvider = ListProvider.create(vertx);
    if (appGuid == null && appUri == null) {

      /**
       * Realms selection
       */
      futureLists = RealmProvider.createFrom(vertx)
        .getRealmFromGuidOrHandle(realmGuid, realmHandle, Realm.class)
        .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
        .compose(listProvider::getListsForRealm);

    } else {

      /**
       * App selections
       */
      Future<App> futureApp;
      if (appGuid != null) {
        futureApp = AppProvider.create(vertx)
          .getAppByGuid(appGuid);
      } else {
        if (realmGuid == null && realmHandle == null) {
          throw ValidationException.create("The realm guid or handle should be given for an appUri", "realmGuid", null);
        }
        futureApp = RealmProvider.createFrom(vertx)
          .getRealmFromGuidOrHandle(realmGuid, realmHandle)
          .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
          .compose(realm -> AppProvider.create(vertx).getAppByUri(URI.create(appUri), realm));
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
  public Future<ApiResponse<java.util.List<ListSummary>>> listsSummaryGet(RoutingContext routingContext, String realmGuid, String realmHandle) {

    if (realmGuid == null && realmHandle == null) {
      throw ValidationException.create("The realm handle or guid should be given", "realmGuid", null);
    }
    Vertx vertx = routingContext.vertx();
    return RealmProvider.createFrom(vertx)
      .getRealmFromGuidOrHandle(realmGuid, realmHandle)
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(realm -> {
        if (realm == null) {
          throw new InternalException("The realm was not found and is mandatory");
        }
        return ListProvider.create(vertx)
          .getListsSummary(realm);
      })
      .onFailure(t -> FailureStatic.failRoutingContextWithTrace(t, routingContext))
      .compose(subscribersSummary -> Future.succeededFuture(new ApiResponse<>(subscribersSummary)));
  }


  @Override
  public Future<ApiResponse<RegistrationList>> listGet(RoutingContext routingContext, String listGuid, String listHandle, String realmHandle) {

    Vertx vertx = routingContext.vertx();
    Future<RegistrationList> listFuture;
    ListProvider listProvider = ListProvider.create(vertx);
    if (listGuid != null) {
      listFuture = listProvider.getListByGuid(listGuid);
    } else {
      if (listHandle == null) {
        throw ValidationException.create("A listGuid or a listHandle is mandatory to retrieve a list", "listGuid", null);
      }
      if (realmHandle == null) {
        throw ValidationException.create("A realm Handle is mandatory to retrieve a list with a listHandle", "realmHandle", null);
      }
      listFuture = RealmProvider.createFrom(vertx)
        .getRealmFromHandle(realmHandle)
        .compose(realm -> listProvider.getListByHandle(listHandle, realm));
    }
    return listFuture
      .compose(registrationList -> {
        RegistrationList registrationListClone = listProvider.toPublicClone(registrationList);
        /**
         * The realm is deleted by default, but we need it on the frontend
         */
        registrationListClone.setRealm(RealmProvider.createFrom(vertx).toPublicClone(registrationList.getRealm()));
        ApiResponse<RegistrationList> apiResult = new ApiResponse<>(registrationListClone);
        return Future.succeededFuture(apiResult);
      });
  }

  @Override
  public Future<ApiResponse<RegistrationList>> listPost(RoutingContext routingContext, ListPostBody publicationPost) {

    Vertx vertx = routingContext.vertx();

    ListProvider listProvider = ListProvider.create(vertx);
    return listProvider
      .postPublication(publicationPost)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(publication -> {
        listProvider.toPublicClone(publication);
        return Future.succeededFuture(new ApiResponse<>(publication));
      });


  }


}
