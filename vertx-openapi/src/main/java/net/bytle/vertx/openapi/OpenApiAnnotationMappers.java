package net.bytle.vertx.openapi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author ckaratza
 * Simple OpenApi Annotation mapping to OpenApi models. This part needs modifications to cover all spec.
 */
final class OpenApiAnnotationMappers {

  private static final Logger log = LoggerFactory.getLogger(OpenApiAnnotationMappers.class);

  static void mergeAnnotationOperationIntoOpenApiOperation(Operation annotation, io.swagger.v3.oas.models.Operation operation) {

    Objects.requireNonNull(operation);

    /**
     * Header
     */
    String operationId = annotation.operationId();
    if (operationId.equals("")) {
      throw new RuntimeException("The operation id should not be null");
    } else {
      operation.operationId(operationId);
    }
    String summary = annotation.summary();
    if (!summary.equals("")) {
      operation.summary(summary);
    }
    String description = annotation.description();
    if (!description.equals("")) {
      operation.description(description);
    }
    operation.deprecated(annotation.deprecated());
    String[] tags = annotation.tags();
    if (tags.length > 0) {
      operation.setTags(Arrays.asList(tags));
    }

    /**
     * Request Body
     */
    io.swagger.v3.oas.models.parameters.RequestBody requestBody = requestBodyAnnotationToOpenApi(annotation.requestBody());
    if (requestBody != null) {
      operation.setRequestBody(requestBody);
    }

    /**
     * Api Response
     */
    ApiResponses responses = processApiResponse(annotation.responses());
    if (!responses.isEmpty()){
      operation.setResponses(responses);
    }

    /**
     * Parameters
     */
    List<Parameter> parameters = parametersAnnotationToOpenApi(annotation.parameters());
    if (parameters.size() > 0) {
      operation.setParameters(parameters);
    }

  }

  private static List<Parameter> parametersAnnotationToOpenApi(io.swagger.v3.oas.annotations.Parameter[] annotationParameters) {
    List<Parameter> parameters = new ArrayList<>();
    for (io.swagger.v3.oas.annotations.Parameter parameter : annotationParameters) {

      Parameter openApiParameter = new Parameter();
      parameters.add(openApiParameter);

      String name = parameter.name();
      if (!name.equals("")) {
        openApiParameter.name(name);
      }

      String description = parameter.description();
      if (!description.equals("")) {
        openApiParameter.description(description);
      }
      openApiParameter.allowEmptyValue(parameter.allowEmptyValue());
      try {
        openApiParameter.style(Parameter.StyleEnum.valueOf(parameter.style().name()));
      } catch (IllegalArgumentException ie) {
        log.warn(ie.getMessage());
      }
      openApiParameter.setRequired(parameter.required());
      openApiParameter.in(parameter.in().name().toLowerCase());

      /**
       * Optional<Schema> schemaFromAnnotation = AnnotationsUtils.getSchemaFromAnnotation(parameter.schema(),null);
       * schemaFromAnnotation.ifPresent(p::schema);
       */
      Schema schema = schemaAnnotationToOpenApi(parameter.schema());
      openApiParameter.schema(schema);

    }
    return parameters;
  }

  private static ApiResponses processApiResponse(io.swagger.v3.oas.annotations.responses.ApiResponse[] responses) {
    ApiResponses apiResponses = new ApiResponses();

    for (io.swagger.v3.oas.annotations.responses.ApiResponse annotationResponse : responses) {

      /**
       * Creation
       */
      String name = annotationResponse.responseCode();
      ApiResponse apiResponse = new ApiResponse();
      apiResponses.addApiResponse(name, apiResponse);
      String description = annotationResponse.description();
      if (!description.equals("")){
        apiResponse.description(description);
      }
      /**
       * Content
       */
      if (annotationResponse.content().length > 0) {
        for (io.swagger.v3.oas.annotations.media.Content content : annotationResponse.content()) {
          Content c = contentAnnotationToOpenApi(content);
          apiResponse.content(c);
        }
      }

      Arrays.stream(annotationResponse.headers()).forEach(header -> {
        Header h = new Header();
        h.description(header.description());
        h.deprecated(header.deprecated());
        //h.allowEmptyValue(header.allowEmptyValue());
        //Optional<Schema> schemaFromAnnotation = AnnotationsUtils.getSchemaFromAnnotation(header.schema());
        //schemaFromAnnotation.ifPresent(h::schema);
        h.required(header.required());
        apiResponse.addHeaderObject(header.name(), h);
      });

    }
    return apiResponses;
  }

  private static Content contentAnnotationToOpenApi(io.swagger.v3.oas.annotations.media.Content contentAnnotation) {
    Schema schema = schemaAnnotationToOpenApi(contentAnnotation.schema());

    Content content = new Content()
      .addMediaType("application/json", new MediaType().schema(schema));

    /**
     * Not sure what this code do
     */
    if (!Void.class.equals(contentAnnotation.array().schema().implementation()))
      content.get(contentAnnotation.mediaType()).getSchema().setExample(clean(contentAnnotation.array().schema().example()));
    else if (!Void.class.equals(contentAnnotation.schema().implementation()))
      content.get(contentAnnotation.mediaType()).getSchema().setExample(contentAnnotation.schema().example());

    return content;

  }

  static io.swagger.v3.oas.models.parameters.RequestBody requestBodyAnnotationToOpenApi(RequestBody requestBody) {

    io.swagger.v3.oas.models.parameters.RequestBody openApiRequestBody = new io.swagger.v3.oas.models.parameters.RequestBody();
    openApiRequestBody.description(requestBody.description());
    openApiRequestBody.setRequired(requestBody.required());
    /**
     * content is an array that contains only one request body
     */
    if (requestBody.content().length == 0) {

      return null;

    } else {

      io.swagger.v3.oas.annotations.media.Content contentAnnotation = requestBody.content()[0];
      Content content = contentAnnotationToOpenApi(contentAnnotation);
      openApiRequestBody.setContent(content);

    }
    return openApiRequestBody;
  }



  private static Object toOpenApiFieldValue(Field field) {
    Class type = field.getType();
    Class componentType = field.getType().getComponentType();

    if (isPrimitiveOrWrapper(type)) {
      return new Schema().type(field.getType().getSimpleName().toLowerCase());
    } else {
      HashMap<String, Object> subMap = new HashMap<>();
      subMap.put("type", "array");
      if (isPrimitiveOrWrapper(componentType)) {
        HashMap<String, Object> arrayMap = new HashMap<String, Object>();
        arrayMap.put("type", componentType.getSimpleName() + "[]");
        subMap.put("type", arrayMap);
      } else {
        if (componentType != null) {
          subMap.put("$ref", "#/components/schemas/" + componentType.getSimpleName());
        }
      }
      return subMap;
    }
  }

  private static Boolean isPrimitiveOrWrapper(Type type) {
    if (type == null) {
      return false;
    }
    return type.equals(Double.class) ||
      type.equals(Float.class) ||
      type.equals(Long.class) ||
      type.equals(Integer.class) ||
      type.equals(Short.class) ||
      type.equals(Character.class) ||
      type.equals(Byte.class) ||
      type.equals(Boolean.class) ||
      type.equals(String.class);
  }

  private static Object clean(final String in) {
    return in;
  }


  /**
   * Schema
   * https://swagger.io/specification/#schema
   */
  private static Schema schemaAnnotationToOpenApi(io.swagger.v3.oas.annotations.media.Schema schemaAnnotation) {

    Schema openApiSchema = new Schema();

    /**
     * Type of schema
     */
    String type = schemaAnnotation.type();
    openApiSchema.setType(type);

    String title = schemaAnnotation.title();
    if (!title.equals("")) {
      openApiSchema.setTitle(title);
    }
    if (!schemaAnnotation.ref().isEmpty()) {
      openApiSchema.set$ref(schemaAnnotation.ref());
    }
    openApiSchema.setDeprecated(schemaAnnotation.deprecated());
    String description = schemaAnnotation.description();
    if (!description.equals("")) {
      openApiSchema.setDescription(description);
    }
    String name = schemaAnnotation.name();
    if (!name.equals("")) {
      openApiSchema.setName(name);
    }

    String format = schemaAnnotation.format();
    if (!format.equals("")) {
      openApiSchema.setFormat(format);
    }

    /**
     * Required
     */
    Field[] requiredFields = FieldUtils.getFieldsListWithAnnotation(schemaAnnotation.implementation(), Required.class).toArray(new Field[0]);
    if (requiredFields.length > 0) {
      List<String> requiredParameters = new ArrayList<>();
      for (Field requiredField : requiredFields) {
        requiredParameters.add(requiredField.getName());
      }
      openApiSchema.required(requiredParameters);
    }

    /**
     * Implementation class shows the property of the object
     */
    Class<?> implementation = schemaAnnotation.implementation();
    if (implementation != java.lang.Void.class) {
      /**
       * There is an `implementation=pojo.class` set
       */
      Field[] fields = implementation.getDeclaredFields();

      Map<String, Object> properties = new HashMap<>();
      for (Field field : fields) {
          properties.put(field.getName(), toOpenApiFieldValue(field));
      }
      if (properties.size() > 0) {
        openApiSchema.setProperties(properties);
      }

    }

    /**
     * Read and set the example
     */
    String annotationExample = schemaAnnotation.example();
    if (!annotationExample.equals("")) {
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        Object example = objectMapper.readValue(annotationExample, Object.class);
        openApiSchema.setExample(example);
      } catch (IOException e) {
        throw new RuntimeException("Unable to parse the string as Json" + annotationExample, e);
      }
    }

    return openApiSchema;
  }



}
