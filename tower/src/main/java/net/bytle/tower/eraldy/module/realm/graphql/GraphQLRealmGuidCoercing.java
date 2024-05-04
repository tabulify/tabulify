package net.bytle.tower.eraldy.module.realm.graphql;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import net.bytle.exception.CastException;
import net.bytle.tower.eraldy.module.realm.model.RealmGuid;
import net.bytle.vertx.jackson.JacksonMapperManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class GraphQLRealmGuidCoercing implements Coercing<RealmGuid, String> {

  private final JacksonMapperManager jacksonMapperManager;

  public GraphQLRealmGuidCoercing(JacksonMapperManager jacksonMapperManager) {
    this.jacksonMapperManager = jacksonMapperManager;
  }

  /**
   * Serialize takes a Java object and converts it into the output shape for that scalar
   */
  @Override
  public @Nullable String serialize(@NotNull Object dataFetcherResult, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingSerializeException {

    return this.jacksonMapperManager.getSerializer(RealmGuid.class).serialize((RealmGuid) dataFetcherResult);

  }

  @Override
  public @Nullable RealmGuid parseLiteral(@NotNull Value<?> input, @NotNull CoercedVariables variables, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseLiteralException {
    return this.parseInput(input);
  }

  @Override
  public @Nullable RealmGuid parseValue(@NotNull Object input, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseValueException {
    return this.parseInput(input);
  }

  private RealmGuid parseInput(Object input) throws CoercingParseLiteralException{
    String string = input.toString();
    try {
      return this.jacksonMapperManager.getDeserializer(RealmGuid.class).deserialize(string);
    } catch (CastException e) {
      throw new CoercingParseLiteralException("The value (" + string + ") is not a valid realm guid. Error: "+e.getMessage(), e);
    }
  }

}
