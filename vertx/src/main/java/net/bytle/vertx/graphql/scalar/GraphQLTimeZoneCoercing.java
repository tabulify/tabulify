package net.bytle.vertx.graphql.scalar;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingSerializeException;
import net.bytle.type.time.TimeZoneCast;
import net.bytle.type.time.TimeZoneUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.TimeZone;

public class GraphQLTimeZoneCoercing implements Coercing<TimeZone, String> {

  /**
   * Serialize takes a Java object and converts it into the output shape for that scalar
   */
  @Override
  public @Nullable String serialize(@NotNull Object dataFetcherResult, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingSerializeException {

    return ((TimeZone) dataFetcherResult).getID();

  }

  @Override
  public @Nullable TimeZone parseLiteral(@NotNull Value<?> input, @NotNull CoercedVariables variables, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseLiteralException {
    String string = input.toString();
    try {
      return TimeZoneUtil.getTimeZoneWithValidation(string);
    } catch (TimeZoneCast e) {
      throw new CoercingParseLiteralException("The value (" + string + ") is not a valid timezone. Error: " + e.getMessage(), e);
    }
  }

}
