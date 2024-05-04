package net.bytle.vertx.graphql.scalar;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import net.bytle.type.Handle;
import net.bytle.type.HandleCastException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class GraphQLHandleCoercing implements Coercing<Handle, String> {

  /**
   * Serialize takes a Java object and converts it into the output shape for that scalar
   */
  @Override
  public @Nullable String serialize(@NotNull Object dataFetcherResult, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingSerializeException {

    /**
     * Not all Handle property in the GraphQlSchema are in a Handle object
     * but string.
     * Migration from string to handle
     */
    if (!(dataFetcherResult instanceof Handle)) {
      return dataFetcherResult.toString();
    }

    return ((Handle) dataFetcherResult).getValue();

  }
  /**
   * Parse a literal (ie a string argument)
   */
  @Override
  public @Nullable Handle parseLiteral(@NotNull Value<?> input, @NotNull CoercedVariables variables, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseLiteralException {
    return this.parseInput(input);
  }
  /**
   * Parse a value (ie a string property in an object argument)
   */
  @Override
  public @Nullable Handle parseValue(@NotNull Object input, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseValueException {
    return this.parseInput(input);
  }

  private Handle parseInput(Object input) throws CoercingParseLiteralException {
    String string = input.toString();
    try {
      return Handle.of(string);
    } catch (HandleCastException e) {
      throw new CoercingParseLiteralException("The value (" + string + ") is not a valid handle. Error: " + e.getMessage(), e);
    }
  }


}
