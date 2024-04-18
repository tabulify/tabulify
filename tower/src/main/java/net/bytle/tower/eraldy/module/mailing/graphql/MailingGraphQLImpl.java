package net.bytle.tower.eraldy.module.mailing.graphql;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.graphql.EraldyGraphQL;
import net.bytle.tower.eraldy.model.openapi.ListObject;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.mailing.db.mailing.MailingProvider;
import net.bytle.tower.eraldy.module.mailing.db.mailingjob.MailingJobProvider;
import net.bytle.tower.eraldy.module.mailing.inputs.MailingInputProps;
import net.bytle.tower.eraldy.module.mailing.inputs.MailingInputTestEmail;
import net.bytle.tower.eraldy.module.mailing.model.Mailing;
import net.bytle.tower.eraldy.module.mailing.model.MailingItem;
import net.bytle.tower.eraldy.module.mailing.model.MailingJob;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.db.JdbcPagination;

import java.util.List;
import java.util.Map;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class MailingGraphQLImpl {


  private final EraldyApiApp app;
  private final MailingProvider mailingProvider;
  private final MailingJobProvider mailingJobProvider;

  public MailingGraphQLImpl(EraldyGraphQL eraldyGraphQL, RuntimeWiring.Builder typeWiringBuilder) {
    this.app = eraldyGraphQL.getApp();
    this.mailingProvider = this.app.getMailingProvider();
    this.mailingJobProvider = this.app.getMailingJobProvider();

    /**
     * Map type to function
     */
    typeWiringBuilder.type(
        newTypeWiring("Query")
          .dataFetcher("mailing", this::getMailing)
          .build()
      )
      .type(
        newTypeWiring("Query")
          .dataFetcher("mailingsOfList", this::getMailingsOfList)
          .build()
      )
      .type(
        newTypeWiring("Query")
          .dataFetcher("mailingJobs", this::getMailingJobs)
          .build()
      )
      .type(
        newTypeWiring("Query")
          .dataFetcher("mailingJob", this::getMailingJob)
          .build()
      )
      /**
       * Data Type mapping
       */
      .type(
        newTypeWiring("Mailing")
          .dataFetcher("emailAuthor", this::getMailingEmailAuthor)
          .build()
      )
      .type(
        newTypeWiring("Mailing")
          .dataFetcher("emailRecipientList", this::getMailingRecipientList)
          .build()
      )
      .type(
        newTypeWiring("Mailing")
          .dataFetcher("items", this::getMailingItems)
          .build()
      )
      /**
       * Mutation
       */
      .type(
        newTypeWiring("Mutation")
          .dataFetcher("mailingUpdate", this::updateMailing)
          .build()
      )
      .type(
        newTypeWiring("Mutation")
          .dataFetcher("mailingCreate", this::createMailing)
          .build()
      )
      .type(
        newTypeWiring("Mutation")
          .dataFetcher("mailingExecute", this::executeMailing)
          .build()
      )
      .type(
        newTypeWiring("Mutation")
          .dataFetcher("mailingSendTestEmail", this::sendTestEmail)
          .build()
      )
      .type(
        newTypeWiring("Mutation")
          .dataFetcher("mailingDeliverItem", this::deliverItem)
          .build()
      );
  }

  private Future<MailingItem> deliverItem(DataFetchingEnvironment dataFetchingEnvironment) {
    String guid = dataFetchingEnvironment.getArgument("guid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app.getMailingItemProvider().getByGuidRequestHandler(guid, routingContext, AuthUserScope.MAILING_DELIVER_ITEM)
      .compose(mailingItem -> {
        if (mailingItem == null) {
          return Future.failedFuture(TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.NOT_FOUND_404)
            .setMessage("The mailing item (" + guid + ") was not found")
            .build()
          );
        }
        return this.app.getMailingFlow().deliverItem(mailingItem, null);
      });
  }

  private Future<List<MailingItem>> getMailingItems(DataFetchingEnvironment dataFetchingEnvironment) {
    Mailing mailing = dataFetchingEnvironment.getSource();
    Map<String, Object> paginationPropsMap = dataFetchingEnvironment.getArgument("pagination");
    // Type safe (if null, the value was not passed)
    JdbcPagination pagination = new JsonObject(paginationPropsMap).mapTo(JdbcPagination.class);
    return this.app.getMailingItemProvider().getItemsForGraphQL(mailing, pagination);
  }

  private Future<MailingJob> getMailingJob(DataFetchingEnvironment dataFetchingEnvironment) {
    String mailingGuid = dataFetchingEnvironment.getArgument("guid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return mailingJobProvider.getMailingJobRequestHandler(mailingGuid, routingContext);
  }

  private Future<List<MailingJob>> getMailingJobs(DataFetchingEnvironment dataFetchingEnvironment) {
    String mailingGuid = dataFetchingEnvironment.getArgument("mailingGuid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return mailingJobProvider.getMailingJobsRequestHandler(mailingGuid, routingContext);
  }

  private Future<MailingJob> executeMailing(DataFetchingEnvironment dataFetchingEnvironment) {
    String guid = dataFetchingEnvironment.getArgument("guid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return mailingProvider.getByGuidRequestHandler(guid, routingContext, AuthUserScope.MAILING_EXECUTE)
      .compose(mailing -> this.app.getMailingFlow()
        .execute(mailing));
  }

  /**
   * Send a test mail
   */
  private Future<Boolean> sendTestEmail(DataFetchingEnvironment dataFetchingEnvironment) {
    String guid = dataFetchingEnvironment.getArgument("guid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    Map<String, Object> mappingPropsMap = dataFetchingEnvironment.getArgument("props");
    // Type safe (if null, the value was not passed)
    MailingInputTestEmail mailingInputProps = new JsonObject(mappingPropsMap).mapTo(MailingInputTestEmail.class);

    return mailingProvider.getByGuidRequestHandler(guid, routingContext, AuthUserScope.MAILING_SEND_TEST_EMAIL)
      .compose(mailing ->{

          User recipient = new User();
          recipient.setEmailAddress(mailingInputProps.getRecipientEmailAddress());

          return this.app.getMailingFlow().sendMail(recipient, mailing)
            .recover(t -> Future.failedFuture(
              TowerFailureException.builder()
                .setMessage("Error while sending the test email. Message: " + t.getMessage())
                .setCauseException(t)
                .build()
            ))
            .compose(mailResult -> Future.succeededFuture(true));

        });
  }





  public Future<Mailing> getMailing(DataFetchingEnvironment dataFetchingEnvironment) {
    String guid = dataFetchingEnvironment.getArgument("guid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return mailingProvider.getByGuidRequestHandler(guid, routingContext, AuthUserScope.MAILING_GET);
  }

  public Future<Mailing> updateMailing(DataFetchingEnvironment dataFetchingEnvironment) {
    String guid = dataFetchingEnvironment.getArgument("guid");
    Map<String, Object> mappingPropsMap = dataFetchingEnvironment.getArgument("props");
    // Type safe (if null, the value was not passed)
    MailingInputProps mailingInputProps = new JsonObject(mappingPropsMap).mapTo(MailingInputProps.class);
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return mailingProvider.updateMailingRequestHandler(guid, mailingInputProps, routingContext);
  }

  public Future<Mailing> createMailing(DataFetchingEnvironment dataFetchingEnvironment) {
    String listGuid = dataFetchingEnvironment.getArgument("listGuid");
    Map<String, Object> mappingPropsMap = dataFetchingEnvironment.getArgument("props");
    // Type safe (if null, the value was not passed)
    MailingInputProps mailingInputProps = new JsonObject(mappingPropsMap).mapTo(MailingInputProps.class);
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return mailingProvider.insertMailingRequestHandler(listGuid, mailingInputProps, routingContext);
  }

  /**
   * Author Late Fetch
   */
  public Future<OrganizationUser> getMailingEmailAuthor(DataFetchingEnvironment dataFetchingEnvironment) {
    Mailing mailing = dataFetchingEnvironment.getSource();
    return this.mailingProvider.buildEmailAuthorAtRequestTimeEventually(mailing);
  }

  /**
   * List Late Fetch
   */
  public Future<ListObject> getMailingRecipientList(DataFetchingEnvironment dataFetchingEnvironment) {
    Mailing mailing = dataFetchingEnvironment.getSource();
    return this.mailingProvider.getListAtRequestTime(mailing);
  }

  public Future<List<Mailing>> getMailingsOfList(DataFetchingEnvironment dataFetchingEnvironment) {
    String listGuid = dataFetchingEnvironment.getArgument("listGuid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);

    Guid guid;
    try {
      guid = this.app.getListProvider().getGuidObject(listGuid);
    } catch (CastException e) {
      return Future.failedFuture(new IllegalArgumentException("The list guid (" + listGuid + ") is not valid", e));
    }

    return this.app.getAuthProvider()
      .getRealmByLocalIdWithAuthorizationCheck(guid.getRealmOrOrganizationId(), AuthUserScope.MAILINGS_LIST_GET, routingContext)
      .compose(realm -> mailingProvider.getMailingsByListWithLocalId(guid.validateRealmAndGetFirstObjectId(realm.getLocalId()), realm));

  }
}
