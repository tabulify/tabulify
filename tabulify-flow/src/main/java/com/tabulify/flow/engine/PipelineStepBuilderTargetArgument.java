package com.tabulify.flow.engine;

import com.tabulify.uri.DataUriStringNode;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * The arguments used in a {@link PipelineStepBuilderTarget target operation}
 * To override them, you need to add them to your argument enum class
 * and be sure that you argument enum class come last in the {@link PipelineStepBuilder#getArgumentEnums()}
 * list
 */
public enum PipelineStepBuilderTargetArgument implements ArgumentEnum {

  TARGET_MEDIA_TYPE("The media type of the target", false, MediaType.class, null),
  TARGET_DATA_URI("A target data uri (Example: table@connection or foo.csv@cd)", true, DataUriStringNode.class, null),
  TARGET_DATA_DEF("The target attributes defined in data definition format", false, Map.class, new HashMap<String, Object>());

  private final String description;
  private final Boolean mandatory;
  private final Class<?> clazz;
  private final Object defaultValue;


  PipelineStepBuilderTargetArgument(String description, Boolean mandatory, Class<?> clazz, Object defaultValue) {

    this.description = description;
    this.mandatory = mandatory;
    this.clazz = clazz;
    this.defaultValue = defaultValue;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return clazz;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }


  public boolean getMandatory() {
    return mandatory;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }

}
