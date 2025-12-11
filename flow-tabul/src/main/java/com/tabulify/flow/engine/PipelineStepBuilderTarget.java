package com.tabulify.flow.engine;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.connection.Connection;
import com.tabulify.spi.DataPath;
import com.tabulify.template.TemplateString;
import com.tabulify.template.TemplateUriFunction;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.exception.NoPathFoundException;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A wrapper for all command that takes a target uri as attribute
 * Target data uri may be used to create a name (transfer) but also to select (diff)
 */
public abstract class PipelineStepBuilderTarget extends PipelineStepBuilder {

  private DataUriNode targetUri;


  /**
   * Target attributes in a map format
   * {@link com.tabulify.spi.DataPathAttribute} and others
   */
  private Map<KeyNormalizer, ?> targetAttributes = new HashMap<>();
  private MediaType targetMediaType;
  private TemplateUriFunction targetUriFunction;
  private Boolean targetIsContainer = false;
  private Set<KeyNormalizer> targetExtraTemplatePrefixes = new HashSet<>();
  /**
   * Do we make the name compatible with the target connection system
   * See {@link com.tabulify.template.TemplateUriFunction.TargetUriFunctionBuilder#targetNameSanitization(Connection, String)}
   */
  private Boolean targetNameSanitization = true;


  /**
   * @param targetUri The target uri (the target to write or compare)
   * @return the step
   */
  public PipelineStepBuilderTarget setTargetDataUri(DataUriNode targetUri) {
    this.targetUri = targetUri;
    return this;
  }

  /**
   * @param bool should we sanitize the target name
   * @return the step
   */
  public PipelineStepBuilderTarget setTargetNameSanitization(Boolean bool) {
    this.targetNameSanitization = bool;
    return this;
  }

  public TemplateString getTargetTemplate() {
    String path;
    try {
      path = this.targetUri.getPath();
    } catch (NoPathFoundException e) {
      path = "";
    }
    return TemplateString
      .builder(path)
      .isStrict(this.getPipeline().isStrict())
      .build();
  }


  /**
   * @param source the source path
   * @return a target from the target template or a random memory target
   */
  public DataPath getTargetFromInput(DataPath source) {
    TemplateUriFunction targetUriFunction = getTargetUriFunction();
    if (targetUriFunction != null) {
      return targetUriFunction.apply(source);
    }
    return getTabular()
      .getAndCreateRandomMemoryDataPath()
      .mergeDataDefinitionFrom(source);

  }


  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    return List.of(PipelineStepBuilderTargetArgument.class);
  }

  /**
   * @param targetAttributes - the data attributes in a yaml format (ie formerly data def)
   */
  public PipelineStepBuilderTarget setTargetDataDef(Map<KeyNormalizer, ?> targetAttributes) {
    this.targetAttributes = targetAttributes;
    return this;
  }

  public PipelineStepBuilderTarget setArgument(KeyNormalizer key, Object value) {

    PipelineStepBuilderTargetArgument targetArgument;
    try {
      targetArgument = Casts.cast(key, PipelineStepBuilderTargetArgument.class);
    } catch (CastException e) {
      String expectedKeyAsString = getArgumentEnums()
        .stream()
        .flatMap(el -> Stream.of(el.getEnumConstants()))
        .map(c -> KeyNormalizer.createSafe(c).toCliLongOptionName())
        .collect(Collectors.joining(", "));

      throw new IllegalArgumentException("The argument (" + key + ") is unknown for the step " + this + ". We were expecting one of " + expectedKeyAsString);
    }
    Attribute attribute;
    try {
      attribute = getTabular().getVault()
        .createVariableBuilderFromAttribute(targetArgument)
        .setOrigin(Origin.PIPELINE)
        .build(value);
      this.setArgument(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + targetArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
    }

    switch (targetArgument) {
      case TARGET_DATA_DEF:
        try {
          this.targetAttributes = Casts.castToNewMap(value, KeyNormalizer.class, Object.class);
        } catch (CastException e) {
          throw new IllegalArgumentException("The " + targetArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
        }
        break;
      case TARGET_DATA_URI:
        DataUriStringNode targetDataUriString = (DataUriStringNode) attribute.getValueOrDefault();
        this.setTargetDataUri(this.getPipelineBuilder().getDataUri(targetDataUriString));
        break;
      case TARGET_MEDIA_TYPE:
        this.setTargetMediaType((MediaType) attribute.getValueOrDefault());
        break;
      case TARGET_NAME_SANITIZATION:
        this.setTargetNameSanitization((Boolean) attribute.getValueOrDefault());
        break;
      default:
        throw new InternalException("The argument `" + key + "` should be processed for the step (" + this + ")");
    }
    return this;
  }

  PipelineStepBuilderTarget setTargetMediaType(MediaType targetMediaType) {
    this.targetMediaType = targetMediaType;
    return this;
  }


  /**
   * @return the target uri function or null if there is no target uri specified
   */
  public TemplateUriFunction getTargetUriFunction() {
    if (this.targetUri == null) {
      /**
       * A target data uri may be set by the class that extends this class
       * How? It just adds it in its own argument enum class
       */
      DataUriStringNode targetUriDefault = (DataUriStringNode) this.getArgument(PipelineStepBuilderTargetArgument.TARGET_DATA_URI)
        .getValueOrDefault();
      if (targetUriDefault == null) {
        return null;
      }
      targetUri = this.getPipelineBuilder().getDataUri(targetUriDefault);
    }
    if (this.targetUriFunction != null) {
      return targetUriFunction;
    }
    targetUriFunction = TemplateUriFunction
      .builder(getTabular())
      .setTargetUri(this.targetUri)
      .setTargetMediaType(this.targetMediaType)
      .setTargetDataDef(this.targetAttributes)
      .setExtraTemplatePrefixes(this.targetExtraTemplatePrefixes)
      .setTargetIsContainer(this.targetIsContainer)
      .setTargetNameSanitization(this.targetNameSanitization)
      .build();
    return targetUriFunction;
  }


  public DataUriNode getTargetUri() {
    return this.targetUri;
  }


  /**
   * Hook that a step can call on build to set that the target is a directory calculation
   *
   * @param targetIsContainer defines if the target is a directory
   */
  protected void setTargetIsContainer(boolean targetIsContainer) {
    this.targetIsContainer = targetIsContainer;
  }

  /**
   * Hook that a step can call on build to add a known template prefixes
   */
  protected PipelineStepBuilderTarget setTargetTemplateExtraPrefixes(Set<KeyNormalizer> templateExtraPrefixed) {
    this.targetExtraTemplatePrefixes = templateExtraPrefixed;
    return this;
  }


}
