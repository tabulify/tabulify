package net.bytle.vertx.graphql.scalar;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingSerializeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Locale;

public class GraphQLUriCoercing implements Coercing<URI, String> {

  /**
   * Serialize takes a Java object and converts it into the output shape for that scalar
   */
  @Override
  public @Nullable String serialize(@NotNull Object dataFetcherResult, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingSerializeException {

    return dataFetcherResult.toString();

  }

  @Override
  public @Nullable URI parseLiteral(@NotNull Value<?> input, @NotNull CoercedVariables variables, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseLiteralException {
    String string = input.toString();
    try {
      return URI.create(string);
    } catch (IllegalArgumentException e) {
      throw new CoercingParseLiteralException("The value (" + string + ") is not a valid URI. Error: " + e.getMessage(), e);
    }
  }

}
