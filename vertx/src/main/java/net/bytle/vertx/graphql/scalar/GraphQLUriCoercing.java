package net.bytle.vertx.graphql.scalar;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
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
  /**
   * Parse a value (ie a string property in an object argument)
   */
  @Override
  public @Nullable URI parseValue(@NotNull Object input, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseValueException {
    return this.parseInput(input);
  }

  /**
   * Parse a literal (ie a string argument)
   */
  @Override
  public @Nullable URI parseLiteral(@NotNull Value<?> input, @NotNull CoercedVariables variables, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseLiteralException {
    return this.parseInput(input);
  }


  private URI parseInput(@NotNull Object input) throws CoercingParseLiteralException {
    String string = input.toString();
    /**
     * HTML forms
     */
    if (string.isBlank()) {
      return null;
    }
    try {
      return URI.create(string);
    } catch (IllegalArgumentException e) {
      throw new CoercingParseLiteralException("The value (" + string + ") is not a valid URI. Error: " + e.getMessage(), e);
    }
  }

}
