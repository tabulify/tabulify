package com.tabulify.flow.operation;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.AttributeEnum;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.AttributeProperties;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.stream.InsertStream;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.Enums;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Return a data path attributes in a line fashion (three columns relation: name, value, description)
 */
public class InfoPipelineStep extends PipelineStepIntermediateMapAbs {


  private final InfoPipelineStepBuilder infoBuilder;

  public InfoPipelineStep(InfoPipelineStepBuilder pipelineStepBuilder) {
    super(pipelineStepBuilder);
    this.infoBuilder = pipelineStepBuilder;
  }


  public static InfoPipelineStepBuilder create() {

    return new InfoPipelineStepBuilder();
  }

  @Override
  public DataPath apply(DataPath dataPath) {

    String feedbackName = "attribute";
    feedbackName = dataPath.getName() + "_" + feedbackName;
    String description = "Information about the data resource (" + dataPath.toDataUri() + ")";

    RelationDef propertiesDataPath = dataPath.getConnection().getTabular().getMemoryConnection()
      .getAndCreateRandomDataPath()
      .setLogicalName(feedbackName)
      .setComment(description)
      .getOrCreateRelationDef();

    // may be use later in a concat
    propertiesDataPath.getDataPath().addAttribute(DataPathAttribute.DATA_URI, dataPath.toDataUri());

    propertiesDataPath
      .addColumn(KeyNormalizer.createSafe(AttributeProperties.ATTRIBUTE).toSqlCase())
      .addColumn(KeyNormalizer.createSafe(AttributeProperties.VALUE).toSqlCase())
      .addColumn(KeyNormalizer.createSafe(AttributeProperties.DESCRIPTION).toSqlCase());

    List<Attribute> attributes = dataPath
      .getAttributes()
      .stream()
      .sorted()
      .toList();
    // They are private attributes that we don't show publicly or with the struct info
    List<AttributeEnum> privateAttribute = Arrays.asList(DataPathAttribute.COLUMNS, DataPathAttribute.PRIMARY_COLUMNS);
    try (InsertStream insertStream = propertiesDataPath.getDataPath().getInsertStream()) {

      for (Attribute attribute : attributes) {
        if (privateAttribute.contains(attribute.getAttributeMetadata())) {
          continue;
        }
        KeyNormalizer attributeNormalized = KeyNormalizer.createSafe(attribute.getAttributeMetadata());
        if (infoBuilder.excludedAttributes.contains(attributeNormalized)) {
          continue;
        }
        Object attributeValue = attribute.getPublicValue().orElse(null);
        List<Object> row = new ArrayList<>();

        // Attribute name printed in a SQL format / in a CLI have the OS case (ie UPPER_SNAKE_CASE)
        // Why Uppercase? we have started to show the tabular and connection attribute in upper snake case
        // to show that they can be set via Operating System variables
        row.add(attributeNormalized.toUpperSnakeCase());
        row.add(attributeValue);
        row.add(attribute.getAttributeMetadata().getDescription());
        insertStream.insert(row);
      }

    }

    return propertiesDataPath.getDataPath();

  }


  public static class InfoPipelineStepBuilder extends PipelineStepBuilder {

    private List<KeyNormalizer> excludedAttributes = new ArrayList<>();

    static final KeyNormalizer INFO = KeyNormalizer.createSafe("info");

    @Override
    public InfoPipelineStepBuilder createStepBuilder() {
      return new InfoPipelineStepBuilder();
    }

    @Override
    public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
      return List.of(InfoPipelineStepArgument.class);
    }

    @Override
    public InfoPipelineStep build() {
      return new InfoPipelineStep(this);
    }

    @Override
    public PipelineStepBuilder setArgument(KeyNormalizer key, Object value) {

      InfoPipelineStepArgument selectArgument;
      try {
        selectArgument = Casts.cast(key, InfoPipelineStepArgument.class);
      } catch (CastException e) {
        throw new IllegalArgumentException("The argument (" + key + ") is not a valid argument for the step (" + this + "). You can choose one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(DropPipelineStepArgument.class));
      }
      Attribute attribute;
      try {
        attribute = this.getPipeline().getTabular().getVault()
          .createVariableBuilderFromAttribute(selectArgument)
          .setOrigin(Origin.PIPELINE)
          .build(value);
        this.setArgument(attribute);
      } catch (CastException e) {
        throw new IllegalArgumentException("The " + selectArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
      }

      switch (selectArgument) {
        case EXCLUDED_ATTRIBUTES:
          try {
            this.setExcludedAttributes(Casts.castToNewList(attribute.getValueOrNull(), String.class));
          } catch (CastException e) {
            throw new IllegalArgumentException("The " + selectArgument + " value (" + value + ") of the step (" + this + ") is not conform (not a list?) . Error: " + e.getMessage(), e);
          }
          break;
        default:
          throw new InternalException("The " + selectArgument + " value (" + value + ") of the step (" + this + ") was not processed");
      }
      return this;
    }

    public InfoPipelineStepBuilder setExcludedAttributes(List<String> excludedAttributes) {
      this.excludedAttributes = excludedAttributes
        .stream()
        .map(KeyNormalizer::createSafe)
        .collect(Collectors.toList());
      return this;
    }

    @Override
    public KeyNormalizer getOperationName() {
      return INFO;
    }
  }
}
