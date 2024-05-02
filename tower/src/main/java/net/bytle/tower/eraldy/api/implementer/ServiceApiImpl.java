package net.bytle.tower.eraldy.api.implementer;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.ServiceApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.Service;
import net.bytle.tower.eraldy.model.openapi.ServiceSmtp;
import net.bytle.tower.eraldy.model.openapi.ServiceSmtpPostBody;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.tower.eraldy.objectProvider.ServiceProvider;
import net.bytle.tower.util.CryptoSymmetricUtil;
import net.bytle.type.Casts;
import net.bytle.vertx.FailureStatic;
import net.bytle.vertx.TowerApp;

import java.util.List;

public class ServiceApiImpl implements ServiceApi {

  private final EraldyApiApp apiApp;

  public ServiceApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
  }

  @Override
  public Future<ApiResponse<Service>> serviceGet(RoutingContext routingContext, String serviceGuid, String serviceUri, String realmIdentifier) {

    ServiceProvider serviceProvider = apiApp.getServiceProvider();
    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifier(realmIdentifier)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(realm -> serviceProvider
        .getServiceByGuidOrUri(serviceGuid, serviceUri, realm))
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(service -> {
        if (service == null) {
          return Future.succeededFuture(new ApiResponse<>(HttpResponseStatus.NOT_FOUND.code()));
        }
        return Future.succeededFuture(new ApiResponse<>(service).setMapper(serviceProvider.getApiMapper()));
      });

  }


  @Override
  public Future<ApiResponse<Service>> serviceSmtpPost(RoutingContext routingContext, ServiceSmtpPostBody serviceSmtpPostBody) {

    Vertx vertx = routingContext.vertx();

    ServiceSmtp serviceSmtp = new ServiceSmtp();
    serviceSmtp.setHost(serviceSmtpPostBody.getSmtpHost());
    serviceSmtp.setPort(serviceSmtpPostBody.getSmtpPort());
    String smtpStartTls = serviceSmtpPostBody.getSmtpStartTls();
    if (smtpStartTls != null) {
      StartTLSOptions startTls;
      try {
        startTls = Casts.cast(smtpStartTls, StartTLSOptions.class);
      } catch (CastException e) {
        throw ValidationException.create("The startTls option is not valid", "smtpStartTls", smtpStartTls);
      }
      serviceSmtp.setStartTls(startTls.name());
    } else {
      serviceSmtp.setStartTls(null);
    }
    serviceSmtp.setUserName(serviceSmtpPostBody.getSmtpUserName());
    String smtpPassword = serviceSmtpPostBody.getSmtpPassword();
    if (smtpPassword != null) {
      smtpPassword = CryptoSymmetricUtil.get(vertx).encrypt(smtpPassword);
      serviceSmtp.setPassword(smtpPassword);
    }

    String impersonatedUserEmail = serviceSmtpPostBody.getImpersonatedUserEmail();
    String impersonatedUserGuid = serviceSmtpPostBody.getImpersonatedUserGuid();
    boolean impersonatedUserWasGiven = impersonatedUserEmail != null || impersonatedUserGuid != null;
    ServiceProvider serviceProvider = apiApp.getServiceProvider();

    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifier(serviceSmtpPostBody.getRealmIdentifier())
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(realm -> {

        Future<User> userFuture = Future.succeededFuture();
        if (impersonatedUserWasGiven) {
          userFuture = apiApp.getUserProvider()
            .getUserFromGuidOrEmail(impersonatedUserGuid, impersonatedUserEmail, realm);
        }

        Future<Realm> realmFuture = Future.succeededFuture(realm);

        return Future.all(userFuture, realmFuture);
      })
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(compositeFuture -> {

        User user = compositeFuture.resultAt(0);

        if (impersonatedUserWasGiven && user == null) {
          if (impersonatedUserGuid != null) {
            throw ValidationException.create("The guid user (" + impersonatedUserGuid + ") was not found", "impersonatedUserGuid", impersonatedUserGuid);
          } else {
            throw ValidationException.create("The email user (" + impersonatedUserEmail + ") was not found", "impersonatedUserEmail", impersonatedUserEmail);
          }
        }

        Service serviceToUpsert = new Service();
        serviceToUpsert.setUri(ServiceProvider.getSmtpUri(serviceSmtp));
        serviceToUpsert.setType(ServiceProvider.SMTP);
        serviceToUpsert.setData(serviceSmtp);
        if (user != null) {
          serviceToUpsert.setImpersonatedUser(user);
        }
        Realm realm = compositeFuture.resultAt(1);
        serviceToUpsert.setRealm(realm);

        return serviceProvider.upsertService(serviceToUpsert);
      })
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(service -> Future.succeededFuture(
        new ApiResponse<>(service)
          .setMapper(serviceProvider.getApiMapper()))
      );
  }


  @Override
  public Future<ApiResponse<List<Service>>> servicesGet(RoutingContext routingContext, String realmIdentifier) {

    ServiceProvider serviceProvider = apiApp.getServiceProvider();
    return this.apiApp.getRealmProvider()
      .getRealmFromIdentifier(realmIdentifier)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(serviceProvider::getServices)
      .onFailure(e -> FailureStatic.failRoutingContextWithTrace(e, routingContext))
      .compose(services -> Future.succeededFuture(
        new ApiResponse<>(services)
          .setMapper(serviceProvider.getApiMapper())
      ));

  }
}
