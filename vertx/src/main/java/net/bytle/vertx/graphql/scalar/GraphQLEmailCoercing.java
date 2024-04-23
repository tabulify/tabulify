package net.bytle.vertx.graphql.scalar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingSerializeException;
import io.vertx.core.json.jackson.DatabindCodec;
import net.bytle.type.EmailAddress;
import net.bytle.type.EmailCastException;
import net.bytle.vertx.jackson.JacksonMapperManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class GraphQLEmailCoercing implements Coercing<EmailAddress, String> {

  /**
   * Serialize takes a Java object and converts it into the output shape for that scalar
   */
  @Override
  public @Nullable String serialize(@NotNull Object dataFetcherResult, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingSerializeException {
    try {
      /**
       * See {@link JacksonMapperManager#registerModuleOnVertxStaticObjectMapper()}
       */
      ObjectMapper jacksonMapper = DatabindCodec.mapper();
      return jacksonMapper.writeValueAsString(dataFetcherResult);
    } catch (JsonProcessingException e) {
      throw new CoercingSerializeException("Unable to serialize an email", e);
    }
  }

  @Override
  public @Nullable EmailAddress parseLiteral(@NotNull Value<?> input, @NotNull CoercedVariables variables, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseLiteralException {
    String string = input.toString();
    try {
      return EmailAddress.of(string);
    } catch (EmailCastException e) {
      throw new CoercingParseLiteralException("The value (" + string + ") is not a valid email", e);
    }
  }

}
