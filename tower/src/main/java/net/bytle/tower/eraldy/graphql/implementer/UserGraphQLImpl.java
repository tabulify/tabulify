package net.bytle.tower.eraldy.graphql.implementer;

import io.vertx.core.Future;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.graphql.EraldyGraphQL;
import net.bytle.tower.eraldy.model.openapi.User;
import org.dataloader.BatchLoaderEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class UserGraphQLImpl {


  private final EraldyApiApp app;

  public UserGraphQLImpl(EraldyGraphQL eraldyGraphQL) {
    this.app = eraldyGraphQL.getApp();
  }

  /**
   * Minimal example that returns empty users
   */
  public CompletionStage<List<User>> batchLoadUsers(List<String> strings, BatchLoaderEnvironment batchLoaderEnvironment) {
    // A list of ids and returns a CompletionStage for a list of users
    Future<List<User>> future = Future.succeededFuture(new ArrayList<>());
    return future.toCompletionStage();
  }

}
