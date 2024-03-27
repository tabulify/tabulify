package net.bytle.tower.eraldy.graphql.implementer;

import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.graphql.EraldyGraphQL;
import net.bytle.tower.eraldy.graphql.input.MailingInputProps;
import net.bytle.tower.eraldy.model.openapi.Mailing;
import net.bytle.tower.eraldy.model.openapi.User;

import java.util.Map;

public class MailingGraphQLImpl {


  private final EraldyApiApp app;

  public MailingGraphQLImpl(EraldyGraphQL eraldyGraphQL) {
    this.app = eraldyGraphQL.getApp();
  }

  public Future<Mailing> getMailing(DataFetchingEnvironment dataFetchingEnvironment) {
    String guid = dataFetchingEnvironment.getArgument("guid");
    RoutingContext routingContext = dataFetchingEnvironment.getGraphQlContext().get(RoutingContext.class);
    return this.app.getMailingProvider().getByGuidRequestHandler(guid,routingContext);
  }

  public Future<Mailing> patchMailing(DataFetchingEnvironment dataFetchingEnvironment) {
    String guid = dataFetchingEnvironment.getArgument("guid");
    Map<String, Object> mappingPropsMap = dataFetchingEnvironment.getArgument("props");
    // Type safe (if null, the value was not passed)
    MailingInputProps mailingInputProps = new JsonObject(mappingPropsMap).mapTo(MailingInputProps.class);

    return Future.succeededFuture(new Mailing());
  }

  public Future<User> getMailingEmailAuthor(DataFetchingEnvironment dataFetchingEnvironment) {
    User user = new User();
    user.setGuid("123");
    return Future.succeededFuture(user);
  }

}
