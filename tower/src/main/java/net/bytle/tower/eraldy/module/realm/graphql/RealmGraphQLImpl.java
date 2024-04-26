package net.bytle.tower.eraldy.module.realm.graphql;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;
import io.vertx.core.Future;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.graphql.EraldyGraphQL;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.module.user.graphql.GraphQLUserGuidCoercing;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class RealmGraphQLImpl {


  private final EraldyApiApp app;


  public RealmGraphQLImpl(EraldyGraphQL eraldyGraphQL, RuntimeWiring.Builder typeWiringBuilder) {
    this.app = eraldyGraphQL.getApp();

    /**
     * Realm Guid scalar
     */
    final GraphQLScalarType REALM_GUID = GraphQLScalarType
      .newScalar()
      .name("RealmGuid")
      .description("The Guid for a realm")
      .coercing(new GraphQLRealmGuidCoercing(this.app.getJackson()))
      .build();
    typeWiringBuilder.scalar(REALM_GUID);

    final GraphQLScalarType USER_GUID = GraphQLScalarType
      .newScalar()
      .name("UserGuid")
      .description("The Guid for a user in the realm")
      .coercing(new GraphQLUserGuidCoercing(this.app.getJackson()))
      .build();
    typeWiringBuilder.scalar(USER_GUID);

    /**
     * Map type to function
     */
    typeWiringBuilder
      /**
       * Data Type mapping
       */
      .type(
        newTypeWiring("Realm")
          .dataFetcher("organization", this::getRealmOrganization)
          .build()
      );

  }

  private Future<Organization> getRealmOrganization(DataFetchingEnvironment dataFetchingEnvironment) {
    Realm realm = dataFetchingEnvironment.getSource();
    return this.app.getRealmProvider().buildOrganizationAtRequestTimeEventually(realm);
  }


}
