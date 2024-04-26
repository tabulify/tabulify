package net.bytle.tower.eraldy.module.list.graphql;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingSerializeException;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.list.model.ListUserGuid;
import net.bytle.vertx.jackson.JacksonMapperManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class GraphQLListUserGuidCoercing implements Coercing<ListUserGuid, String> {

  private final JacksonMapperManager jacksonMapperManager;

  public GraphQLListUserGuidCoercing(JacksonMapperManager jacksonMapperManager) {
    this.jacksonMapperManager = jacksonMapperManager;
  }

  /**
   * Serialize takes a Java object and converts it into the output shape for that scalar
   */
  @Override
  public @Nullable String serialize(@NotNull Object dataFetcherResult, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingSerializeException {

    return this.jacksonMapperManager.getSerializer(ListUserGuid.class).serialize((ListUserGuid) dataFetcherResult);

  }

  @Override
  public @Nullable ListUserGuid parseLiteral(@NotNull Value<?> input, @NotNull CoercedVariables variables, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseLiteralException {
    String string = input.toString();
    try {
      return this.jacksonMapperManager.getDeserializer(ListUserGuid.class).deserialize(string);
    } catch (CastException e) {
      throw new CoercingParseLiteralException("The value (" + string + ") is not a valid list user guid", e);
    }
  }

}
