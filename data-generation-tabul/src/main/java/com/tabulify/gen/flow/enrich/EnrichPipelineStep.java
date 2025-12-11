package com.tabulify.gen.flow.enrich;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.AttributeEnum;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.gen.DataSupplierAttribute;
import com.tabulify.gen.GenColumnAttribute;
import com.tabulify.gen.generator.MetaAttributeArgument;
import com.tabulify.gen.generator.MetaAttributeGenerator;
import com.tabulify.model.ColumnAttribute;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.Enums;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The enrich step is in the data generation module
 * because the columns may be data generated
 * (ie time now, sequence and the like)
 */
public class EnrichPipelineStep extends PipelineStepBuilder {

  private static final KeyNormalizer ENRICH = KeyNormalizer.createSafe("enrich");
  public static final KeyNormalizer COLUMNS = KeyNormalizer.createSafe(DataPathAttribute.COLUMNS);

  /**
   * To be used with {@link DataPath#mergeDataDefinitionFromYamlMap(Map)}
   */
  Map<KeyNormalizer, Object> yamlMap = new HashMap<>();

  public static EnrichPipelineStep builder() {
    return new EnrichPipelineStep();
  }


  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    return List.of(EnrichPipelineStepArgument.class);
  }


  @Override
  public EnrichPipelineStep createStepBuilder() {
    return new EnrichPipelineStep();
  }


  @Override
  public EnrichPipelineStepMap build() {
    return new EnrichPipelineStepMap(this);
  }

  @Override
  public EnrichPipelineStep setArgument(KeyNormalizer key, Object value) {

    EnrichPipelineStepArgument enrichArgument;
    try {
      enrichArgument = Casts.cast(key, EnrichPipelineStepArgument.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The argument (" + key + ") is not valid argument for the step (" + this + "). We were expecting " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(EnrichPipelineStepArgument.class));
    }
    Attribute attribute;
    try {
      attribute = this.getTabular().getVault()
        .createVariableBuilderFromAttribute(enrichArgument)
        .setOrigin(Origin.PIPELINE)
        .build(value);
    } catch (CastException e) {
      throw new IllegalArgumentException("The argument value (" + enrichArgument + ") is not valid for the step (" + this + "). Error: " + e.getMessage(), e);
    }
    this.setArgument(attribute);

    switch (enrichArgument) {

      case DATA_DEF:
        try {
          yamlMap = Casts.castToNewMap(value, KeyNormalizer.class, Object.class);
        } catch (CastException e) {
          throw new RuntimeException(e);
        }
        break;

      default:
        throw new InternalException("The enrichArgument (" + enrichArgument + ") was not processed");

    }
    return this;

  }


  @Override
  public KeyNormalizer getOperationName() {
    return ENRICH;
  }

  public EnrichPipelineStep addMetaColumns(Map<String, String> columnNameAttributeMap) {

    for (Map.Entry<String, String> entry : columnNameAttributeMap.entrySet()) {
      this.addMetaColumn(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public EnrichPipelineStep addMetaColumn(String columnName, String attribute) {
    //noinspection unchecked
    List<Map<String, Object>> arrayList = (List<Map<String, Object>>) yamlMap.computeIfAbsent(COLUMNS, (c) -> new ArrayList<Map<String, Object>>());

    /**
     * Key String and not KeyNormalizer as this is what expect the
     * {@link DataPath#mergeDataDefinitionFromYamlMap(Map)}
     */
    Map<String, Object> dataSupplier = new HashMap<>();
    dataSupplier.put(DataSupplierAttribute.TYPE.toString(), MetaAttributeGenerator.TYPE.toString());
    Map<String, Object> dataSupplierArgument = new HashMap<>();
    dataSupplier.put(DataSupplierAttribute.ARGUMENTS.toString(), dataSupplierArgument);
    dataSupplierArgument.put(MetaAttributeArgument.ATTRIBUTE.toString(), attribute);

    /**
     * Key String and not KeyNormalizer as this is what expect the
     * {@link DataPath#mergeDataDefinitionFromYamlMap(Map)}
     */
    Map<String, Object> columnDef = new HashMap<>();
    columnDef.put(ColumnAttribute.NAME.toString(), columnName);
    columnDef.put(GenColumnAttribute.DATA_SUPPLIER.toString(), dataSupplier);

    arrayList.add(columnDef);
    return this;
  }

  public EnrichPipelineStep addMetaColumn(String columnName, AttributeEnum attributeEnum) {
    return this.addMetaColumn(columnName, attributeEnum.name());
  }
}
