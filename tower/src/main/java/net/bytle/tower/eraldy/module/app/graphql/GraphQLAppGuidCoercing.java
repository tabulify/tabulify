package net.bytle.tower.eraldy.module.app.graphql;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingSerializeException;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.app.model.AppGuid;
import net.bytle.vertx.jackson.JacksonMapperManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class GraphQLAppGuidCoercing implements Coercing<AppGuid, String> {

  private final JacksonMapperManager jacksonMapperManager;

  public GraphQLAppGuidCoercing(JacksonMapperManager jacksonMapperManager) {
    this.jacksonMapperManager = jacksonMapperManager;
  }

  /**
   * Serialize takes a Java object and converts it into the output shape for that scalar
   */
  @Override
  public @Nullable String serialize(@NotNull Object dataFetcherResult, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingSerializeException {

    return this.jacksonMapperManager.getSerializer(AppGuid.class).serialize((AppGuid) dataFetcherResult);

  }

  @Override
  public @Nullable AppGuid parseLiteral(@NotNull Value<?> input, @NotNull CoercedVariables variables, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseLiteralException {
    String string = input.toString();
    try {
      return this.jacksonMapperManager.getDeserializer(AppGuid.class).deserialize(string);
    } catch (CastException e) {
      throw new CoercingParseLiteralException("The value (" + string + ") is not a valid app guid", e);
    }
  }

}
