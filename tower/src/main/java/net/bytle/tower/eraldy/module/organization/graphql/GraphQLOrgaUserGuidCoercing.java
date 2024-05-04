package net.bytle.tower.eraldy.module.organization.graphql;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.vertx.jackson.JacksonMapperManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class GraphQLOrgaUserGuidCoercing implements Coercing<OrgaUserGuid, String> {

  private final JacksonMapperManager jacksonMapperManager;

  public GraphQLOrgaUserGuidCoercing(JacksonMapperManager jacksonMapperManager) {
    this.jacksonMapperManager = jacksonMapperManager;
  }

  /**
   * Serialize takes a Java object and converts it into the output shape for that scalar
   */
  @Override
  public @Nullable String serialize(@NotNull Object dataFetcherResult, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingSerializeException {

    return this.jacksonMapperManager.getSerializer(OrgaUserGuid.class).serialize((OrgaUserGuid) dataFetcherResult);

  }

  /**
   * Parse a value from an input
   */
  @Override
  public @Nullable OrgaUserGuid parseValue(@NotNull Object input, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseValueException {
    return this.parseInput(input);
  }

  @Override
  public @Nullable OrgaUserGuid parseLiteral(@NotNull Value<?> input, @NotNull CoercedVariables variables, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseLiteralException {
    return this.parseInput(input);
  }

  private OrgaUserGuid parseInput(Object input) {
    String string = input.toString();
    try {
      return this.jacksonMapperManager.getDeserializer(OrgaUserGuid.class).deserialize(string);
    } catch (CastException e) {
      throw new CoercingParseLiteralException("The value (" + string + ") is not a valid orga user guid", e);
    }
  }
}
