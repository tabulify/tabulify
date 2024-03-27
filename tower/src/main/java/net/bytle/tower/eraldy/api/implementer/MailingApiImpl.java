package net.bytle.tower.eraldy.api.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.api.openapi.interfaces.MailingApi;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.MailingProvider;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;

public class MailingApiImpl implements MailingApi {


  private final EraldyApiApp apiApp;

  public MailingApiImpl(TowerApp towerApp) {
    this.apiApp = (EraldyApiApp) towerApp;
  }

  @Override
  public Future<ApiResponse<Email>> mailingIdentifierEmailGet(RoutingContext routingContext, String mailingIdentifier) {
    MailingProvider mailingProvider = this.apiApp.getMailingProvider();
    Guid guid;
    try {
      guid = mailingProvider.getGuid(mailingIdentifier);
    } catch (CastException e) {
      return Future.failedFuture(new IllegalArgumentException("The mailing guid (" + mailingIdentifier + ") is not valid", e));
    }
    return this.apiApp.getRealmProvider()
      .getRealmFromLocalId(guid.getRealmOrOrganizationId())
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404) // our fault?, deleted a realm is pretty rare.
            .setMessage("The realm of the mailing (" + mailingIdentifier + ") was not found")
            .build()
          );
        }
        return this.apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthUserScope.MAILING_EMAIL_GET);
      })
      .compose(realm -> {
        long localId = guid.validateRealmAndGetFirstObjectId(realm.getLocalId());
        return mailingProvider.getByLocalId(localId, realm);
      })
      .compose(mailing -> {
        if (mailing == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404) // our fault
            .setMessage("The mailing (" + mailingIdentifier + ") was not found")
            .build()
          );
        }
        if (mailing.getEmailFileId() == null) {
          Email email = new Email();
          // null == undefined
          email.setSubject(null);
          email.setBody(null);
          email.setPreview(null);
          return Future.succeededFuture(new ApiResponse<>(email));
        }
        return Future.succeededFuture();
      });
  }

  public Future<ApiResponse<Void>> mailingIdentifierEmailPost(RoutingContext routingContext, String mailingIdentifier, MailingEmailPost mailingEmailPost) {
    MailingProvider mailingProvider = this.apiApp.getMailingProvider();
    Guid guid;
    try {
      guid = mailingProvider.getGuid(mailingIdentifier);
    } catch (CastException e) {
      return Future.failedFuture(new IllegalArgumentException("The mailing guid (" + mailingIdentifier + ") is not valid", e));
    }
    return Future.succeededFuture();
  }

  @Override
  public Future<ApiResponse<Mailing>> mailingIdentifierGet(RoutingContext routingContext, String mailingGuidIdentifier) {

    MailingProvider mailingProvider = this.apiApp.getMailingProvider();
    return mailingProvider
      .getByGuidRequestHandler(mailingGuidIdentifier, routingContext)
      .compose(mailing -> {
        if (mailing == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404) // our fault
            .setMessage("The mailing (" + mailingGuidIdentifier + ") was not found")
            .build()
          );
        }
        return Future.succeededFuture(new ApiResponse<>(mailing).setMapper(mailingProvider.getApiMapper()));
      });

  }

  @Override
  public Future<ApiResponse<Mailing>> mailingIdentifierPost(RoutingContext routingContext, String mailingGuidIdentifier, MailingUpdatePost mailingUpdatePost) {
    MailingProvider mailingProvider = this.apiApp.getMailingProvider();
    Guid guid;
    try {
      guid = mailingProvider.getGuid(mailingGuidIdentifier);
    } catch (CastException e) {
      return Future.failedFuture(new IllegalArgumentException("The mailing guid (" + mailingGuidIdentifier + ") is not valid", e));
    }

    return this.apiApp.getRealmProvider()
      .getRealmFromLocalId(guid.getRealmOrOrganizationId())
      .recover(err -> Future.failedFuture(new InternalException("Error on realm get. Error: " + err.getMessage(), err)))
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404) // our fault?, deleted a realm is pretty rare.
            .setMessage("The realm of the mailing (" + mailingGuidIdentifier + ") was not found")
            .build()
          );
        }
        return this.apiApp.getAuthProvider().checkRealmAuthorization(routingContext, realm, AuthUserScope.MAILING_UPDATE);
      })
      .recover(err -> Future.failedFuture(new InternalException("Error on authorization check. Error: " + err.getMessage(), err)))
      .compose(realm -> {
        long localId = guid.validateRealmAndGetFirstObjectId(realm.getLocalId());
        return mailingProvider.getByLocalId(localId, realm);
      })
      .recover(err -> Future.failedFuture(new InternalException("Error on mailing user get. Error: " + err.getMessage(), err)))
      .compose(mailing -> {
        if (mailing == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404)
            .setMessage("The mailing (" + mailingGuidIdentifier + ") was not found")
            .build()
          );
        }
        Future<OrganizationUser> futureUser = this.apiApp.getOrganizationUserProvider()
          .getOrganizationUserByIdentifier(mailingUpdatePost.getAuthorUserGuid());
        return Future.all(Future.succeededFuture(mailing), futureUser);
      })
      .recover(err -> Future.failedFuture(new InternalException("Error on mailing user getting for mailing update. Error: " + err.getMessage(), err)))
      .compose(compositeFuture -> {
        if (compositeFuture.failed()) {
          // user get error as mailing is already succeeded
          return Future.failedFuture(new InternalException("Error on composite mailing user getting for mailing update. Error: " + compositeFuture.cause()));
        }
        Mailing mailing = compositeFuture.resultAt(0);
        OrganizationUser organizationUser = compositeFuture.resultAt(1);
        if (organizationUser == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404)
            .setMessage("The organisation user (" + mailingUpdatePost.getAuthorUserGuid() + ") was not found")
            .build()
          );
        }

        /**
         * Update
         */
        mailing.setName(mailingUpdatePost.getName());
        mailing.setEmailAuthor(organizationUser);
        return mailingProvider.updateMailingNameAndAuthor(mailing);

      })
      .compose(mailing -> Future.succeededFuture(new ApiResponse<>(mailing).setMapper(mailingProvider.getApiMapper())));
  }

}
