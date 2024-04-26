package net.bytle.tower.eraldy.module.organization.graphql;

import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;
import net.bytle.tower.eraldy.graphql.EraldyGraphQL;

public class OrgaGraphQLImpl {




  public OrgaGraphQLImpl(EraldyGraphQL eraldyGraphQL, RuntimeWiring.Builder wiringBuilder) {


    final GraphQLScalarType ORGA_GUID = GraphQLScalarType
      .newScalar()
      .name("OrgaGuid")
      .description("The Guid for a organization")
      .coercing(new GraphQLOrgaGuidCoercing(eraldyGraphQL.getApp().getJackson()))
      .build();
    wiringBuilder.scalar(ORGA_GUID);

    final GraphQLScalarType ORGA_USER_GUID = GraphQLScalarType
      .newScalar()
      .name("OrgaUserGuid")
      .description("The Guid for the user of an organization")
      .coercing(new GraphQLOrgaUserGuidCoercing(eraldyGraphQL.getApp().getJackson()))
      .build();
    wiringBuilder.scalar(ORGA_USER_GUID);

  }


}
