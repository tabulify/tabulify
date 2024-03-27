package net.bytle.vertx.graphql;


import graphql.Scalars;
import graphql.schema.*;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * LocalDate Formatting
 * <a href="https://www.graphql-java.com/documentation/sdl-directives#another-example---date-formatting">...</a>
 */
public class GraphQLLocalDate implements SchemaDirectiveWiring {


  private final DateTimeFormatter defaultDateTimeFormatter;
  private final String defaultArgumentValue;
  private final String argumentName;

  public GraphQLLocalDate() {
    this.argumentName = "format";
    this.defaultArgumentValue = "yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnn"; // ISO
    this.defaultDateTimeFormatter = DateTimeFormatter.ofPattern(this.defaultArgumentValue);
  }

  @Override
  public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
    GraphQLFieldDefinition field = environment.getElement();

    GraphQLFieldsContainer parentType = environment.getFieldsContainer();
    //
    // DataFetcherFactories.wrapDataFetcher is a helper to wrap data fetchers so that CompletionStage is handled correctly
    // along with POJOs
    //
    //noinspection rawtypes
    DataFetcher originalFetcher = environment.getCodeRegistry().getDataFetcher(parentType, field);
    //noinspection rawtypes
    DataFetcher dataFetcher = DataFetcherFactories.wrapDataFetcher(originalFetcher, ((dataFetchingEnvironment, value) -> {
      if (!(value instanceof LocalDateTime)) {
        return value;
      }
      String format = dataFetchingEnvironment.getArgument(argumentName);
      DateTimeFormatter dateTimeFormatter;
      if (format == null || format.equals(this.defaultArgumentValue)) {
        dateTimeFormatter = this.defaultDateTimeFormatter;
      } else {
        dateTimeFormatter = DateTimeFormatter.ofPattern(format);
      }
      return dateTimeFormatter.format((LocalDateTime) value);


    }));

    //
    // This will extend the field by adding a new "format" argument to it for the date formatting
    // which allows clients to opt into that as well as wrapping the base data fetcher so it
    // performs the formatting over top of the base values.
    //
    FieldCoordinates coordinates = FieldCoordinates.coordinates(parentType, field);
    environment.getCodeRegistry().dataFetcher(coordinates, dataFetcher);

    return field.transform(builder -> builder
      .argument(GraphQLArgument
        .newArgument()
        .name(argumentName)
        .type(Scalars.GraphQLString)
        .defaultValueProgrammatic(this.defaultArgumentValue)
      )
    );
  }

}
