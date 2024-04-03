package net.bytle.tower.eraldy.graphql.implementer;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.graphql.EraldyGraphQL;
import net.bytle.tower.eraldy.graphql.pojo.input.MailingInputProps;
import net.bytle.tower.eraldy.model.manual.Mailing;
import net.bytle.tower.eraldy.model.openapi.ListObject;
import net.bytle.tower.eraldy.model.openapi.OrganizationUser;
import net.bytle.tower.eraldy.objectProvider.MailingProvider;
import net.bytle.tower.util.Guid;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerSmtpClientService;

import java.util.List;
import java.util.Map;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class MailingGraphQLImpl {


  private final EraldyApiApp app;
  private final MailingProvider mailingProvider;

  public MailingGraphQLImpl(EraldyGraphQL eraldyGraphQL, RuntimeWiring.Builder typeWiringBuilder) {
    this.app = eraldyGraphQL.getApp();
    this.mailingProvider = this.app.getMailingProvider();

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
        newTypeWiring("Mutation")
          .dataFetcher("mailingSendTestEmail", this::sendTestEmail)
          .build()
      );
  }

  private Future<Boolean> sendTestEmail(DataFetchingEnvironment dataFetchingEnvironment) {
    String guid = dataFetchingEnvironment.getArgument("guid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return mailingProvider.getByGuidRequestHandler(guid, routingContext, AuthUserScope.MAILING_SEND_TEST_EMAIL)
      .compose(mailing -> this.app.getMailingProvider().getEmailAuthorAtRequestTime(mailing)
        .compose(emailAuthor -> {

          TowerSmtpClientService smtpClientService = this.app.getEmailSmtpClientService();
          String emailAddressAsString = emailAuthor.getEmailAddress();
          EmailAddress emailAddress;
          try {
            emailAddress = new EmailAddress(emailAddressAsString);
          } catch (EmailCastException e) {
            return Future.failedFuture(new InternalException("The email (" + emailAddressAsString + ") of the author is invalid", e));
          }

          MailMessage email = smtpClientService.createVertxMailMessage()
            .setTo(emailAddressAsString)
            .setFrom(emailAddressAsString)
            .setSubject(mailing.getEmailSubject())
            .setText(mailing.getEmailBody());

          return smtpClientService
            .getVertxMailClientForSenderWithSigning(emailAddress.getDomainName().toStringWithoutRoot())
            .sendMail(email)
            .recover(t -> Future.failedFuture(
              TowerFailureException.builder()
                .setMessage("Error while sending the test email. Message: " + t.getMessage())
                .setCauseException(t)
                .build()
            ))
            .compose(mailResult -> Future.succeededFuture(true));

        }));
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
    return this.mailingProvider.getEmailAuthorAtRequestTime(mailing);
  }

  /**
   * List Late Fetch
   */
  public Future<ListObject> getMailingRecipientList(DataFetchingEnvironment dataFetchingEnvironment) {
    ListObject listObject = ((Mailing) dataFetchingEnvironment.getSource()).getEmailRecipientList();
    String guid = listObject.getGuid();
    if (guid != null) {
      return Future.succeededFuture(listObject);
    }
    return this.app.getListProvider()
      .getListById(listObject.getLocalId(), listObject.getRealm());
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
      .checkRealmAuthorization(routingContext, guid.getRealmOrOrganizationId(), AuthUserScope.MAILINGS_LIST_GET)
      .compose(realmId -> mailingProvider.getMailingsByListWithLocalId(guid.validateRealmAndGetFirstObjectId(realmId), realmId));

  }
}
