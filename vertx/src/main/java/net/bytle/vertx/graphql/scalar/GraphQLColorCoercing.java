package net.bytle.vertx.graphql.scalar;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingSerializeException;
import net.bytle.type.Color;
import net.bytle.type.ColorCastException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class GraphQLColorCoercing implements Coercing<Color, String> {

  /**
   * Serialize takes a Java object and converts it into the output shape for that scalar
   */
  @Override
  public @Nullable String serialize(@NotNull Object dataFetcherResult, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingSerializeException {


    return ((Color) dataFetcherResult).getValue();

  }

  @Override
  public @Nullable Color parseLiteral(@NotNull Value<?> input, @NotNull CoercedVariables variables, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseLiteralException {
    String string = input.toString();
    try {
      return Color.of(string);
    } catch (ColorCastException e) {
      throw new CoercingParseLiteralException("The value (" + string + ") is not a valid color. Error: " + e.getMessage(), e);
    }
  }

}
