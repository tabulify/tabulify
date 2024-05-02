package net.bytle.tower.eraldy.module.app.graphql;

import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;
import net.bytle.tower.eraldy.graphql.EraldyGraphQL;

public class GraphQLApp {


  public GraphQLApp(EraldyGraphQL eraldyGraphQL, RuntimeWiring.Builder wiringBuilder) {

    final GraphQLScalarType APP_GUID = GraphQLScalarType
      .newScalar()
      .name("AppGuid")
      .description("The guid for an app")
      .coercing(new GraphQLAppGuidCoercing(eraldyGraphQL.getApp().getJackson()))
      .build();
    wiringBuilder.scalar(APP_GUID);

  }
}
