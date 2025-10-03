package net.bytle.template.flow;

import com.tabulify.flow.Granularity;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.flow.operation.StepOutputArgument;
import com.tabulify.template.TemplateString;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.type.*;
import net.bytle.type.yaml.YamlCast;

import java.util.*;

import static net.bytle.template.flow.TemplateAttribute.*;

public class TemplatePipelineStep extends PipelineStepBuilderTarget {


  static final KeyNormalizer TEMPLATE = KeyNormalizer.createSafe("template");
  /**
   * The environment data path variable
   * from the template are added to the engine
   * with this prefix
   */
  public static final KeyNormalizer TEMPLATE_PREFIX = KeyNormalizer.createSafe("template");
  final List<DataUriNode> templateSelectors = new ArrayList<>();
  final Map<String, TemplateModel> inlineTemplates = new HashMap<>();
  public String column_name_template;
  Granularity granularity;
  TemplateEngine templateEngine = (TemplateEngine) TEMPLATE_ENGINE.getDefaultValue();
  final Map<String, DataUriNode> modelVariables = new HashMap<>();
  TemplateTargetType templateTargetType = (TemplateTargetType) TARGET_TYPE.getDefaultValue();
  Boolean isTemplateEmail = (Boolean) TEMPLATE_EMAIL.getDefaultValue();
  private StepOutputArgument output = (StepOutputArgument) OUTPUT_TYPE.getDefaultValue();

  /**
   * Target column name
   */
  private String targetColumnName = (String) TARGET_COLUMN_NAME.getDefaultValue();
  TemplateString targetColumnNameTemplateFunction;


  ProcessingType processingType;


  public TemplatePipelineStep addTemplateSelectors(Set<DataUriNode> dataSelectors) {
    this.templateSelectors.addAll(dataSelectors);
    return this;
  }

  public static TemplatePipelineStep builder() {
    return new TemplatePipelineStep();
  }


  public TemplatePipelineStep addTemplateSelector(DataUriNode dataSelector) {
    this.templateSelectors.add(dataSelector);
    return this;
  }

  public TemplatePipelineStep setTemplateEngine(TemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
    return this;
  }

  public TemplatePipelineStep addTemplateModelVariable(String name, DataUriNode tableSelector) {
    this.modelVariables.put(name, tableSelector);
    return this;
  }


  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    ArrayList<Class<? extends ArgumentEnum>> classes = new ArrayList<>(super.getArgumentEnums());
    classes.add(TemplateAttribute.class);
    return classes;
  }

  @Override
  public TemplatePipelineStep setArgument(KeyNormalizer key, Object value) {

    TemplateAttribute templateAttribute;
    try {
      templateAttribute = Casts.cast(key, TemplateAttribute.class);
    } catch (CastException e) {
      super.setArgument(key, value);
      return this;
    }
    switch (templateAttribute) {
      case TEMPLATE_INLINE:
        MapKeyIndependent<String> mapInlineTemplate = YamlCast.castToMapKeyIndependent(value, String.class);
        String inLineTemplateContent = null;
        String inlineTemplateLogicalName = "anonymous";
        MediaType type = null;
        for (Map.Entry<String, String> entryInlineTemplate : mapInlineTemplate.entrySet()) {

          String attributeKey = entryInlineTemplate.getKey();
          TemplateInlineAttribute templateInlineAttribute;
          try {
            templateInlineAttribute = Casts.cast(attributeKey, TemplateInlineAttribute.class);
          } catch (CastException e) {
            throw new IllegalArgumentException("The template inline attribute (" + attributeKey + ") is not valid. We were expecting one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(TemplateInlineAttribute.class), e);
          }
          switch (templateInlineAttribute) {
            case MEDIA_TYPE:
              try {
                type = MediaTypes.parse(entryInlineTemplate.getValue());
              } catch (NullValueException e) {
                throw IllegalArgumentExceptions.createFromMessage("The media type value was null for the template step (" + this + ")");
              }
              break;
            case CONTENT:
              inLineTemplateContent = entryInlineTemplate.getValue();
              break;
            case LOGICAL_NAME:
              inlineTemplateLogicalName = entryInlineTemplate.getValue();
              break;
            default:
              throw new InternalException("The inline template property `" + templateInlineAttribute + "` of the step (" + this + ") is not in the switch branch.");
          }
        }
        if (inLineTemplateContent == null) {
          throw new IllegalArgumentException("The inline template of the step (" + this + ") does not have any `content` property. We can't therefore define a template");
        }
        if (type == null) {
          throw new IllegalArgumentException("The type of the inline template could not be found");
        }
        if (this.inlineTemplates.containsKey(inlineTemplateLogicalName)) {
          throw new IllegalArgumentException("The template with the logical name (" + inlineTemplateLogicalName + ") has already been defined once");
        }
        TemplateModel templateProperties = TemplateModel
          .create()
          .setMediaType(type)
          .setContent(inLineTemplateContent);
        this.inlineTemplates.put(inlineTemplateLogicalName, templateProperties);
        break;
      case TEMPLATE_SELECTOR:
        String templateSelector = value.toString();
        try {
          DataUriStringNode dataUriString = DataUriStringNode.createFromString(templateSelector);
          this.templateSelectors.add(this.getPipelineBuilder().getDataUri(dataUriString));
        } catch (CastException e) {
          throw new IllegalArgumentException("The value (" + templateSelector + ") of the attribute (" + key + ") is not a valid data URI string. Error: " + e.getMessage());
        }
        break;
      case TEMPLATE_SELECTORS:

        List<String> dataSelectors;
        try {
          dataSelectors = Casts.castToNewList(value, String.class);
        } catch (CastException e) {
          throw new IllegalArgumentException("The value of the attribute (" + key + ") is not a list of string. Error: " + e.getMessage());
        }
        for (String dataSelector : dataSelectors) {
          try {
            DataUriStringNode dataUriString = DataUriStringNode.createFromString(dataSelector);
            this.templateSelectors.add(this.getPipelineBuilder().getDataUri(dataUriString));
          } catch (CastException e) {
            throw new IllegalArgumentException("The value (" + dataSelector + ") of the attribute (" + key + ") is not a valid data URI string. Error: " + e.getMessage());
          }
        }
        break;
      case GRANULARITY:
        try {
          this.granularity = Casts.cast(value.toString(), Granularity.class);
        } catch (CastException e) {
          throw IllegalArgumentExceptions.createForArgumentValueForStep(value.toString(), templateAttribute, this.toString(), Granularity.class, e);
        }
        break;
      case MODEL_VARIABLES:
        if (!(value instanceof List)) {
          throw new IllegalStateException("The " + MODEL_VARIABLES + " attribute is not a list but a " + value.getClass().getSimpleName());
        }
        List<Object> tableVariables = Casts.castToNewListSafe(value, Object.class);
        for (Object tableVariable : tableVariables) {
          if (!(tableVariable instanceof Map)) {
            throw new IllegalStateException("A table variable value is not a map but a " + value.getClass().getSimpleName());
          }
          Map<String, String> tableVariableMap;
          try {
            tableVariableMap = Casts.castToSameMap(tableVariable, String.class, String.class);
          } catch (CastException e) {
            throw new InternalException("String, string should not throw an exception", e);
          }
          String variableName = tableVariableMap.get("name");
          String variableDataSelectors = tableVariableMap.get("selectors");
          this.addTemplateModelVariable(variableName, getTabular().createDataUri(variableDataSelectors));
        }
        break;
      case PROCESSING_TYPE:
        try {
          this.setProcessingType(Casts.cast(value.toString(), ProcessingType.class));
        } catch (CastException e) {
          throw IllegalArgumentExceptions.createForArgumentValueForStep(value.toString(), templateAttribute, this.toString(), ProcessingType.class, e);
        }
        break;
      case OUTPUT_TYPE:
        if (!(value instanceof String)) {
          throw new IllegalStateException("The output operation is not a string but a " + value.getClass().getSimpleName());
        }
        try {
          this.setOutput(Casts.cast(value.toString(), StepOutputArgument.class));
        } catch (CastException e) {
          throw IllegalArgumentExceptions.createForArgumentValueForStep(value.toString(), templateAttribute, this.toString(),
            StepOutputArgument.class, e);
        }
        break;
      case TEMPLATE_EMAIL:
        Boolean b = Booleans.createFromString(value.toString()).toBoolean();
        this.setIsTemplateEmail(b);
        break;
      default:
        throw new InternalException("The attribute (" + key + ") of the step (" + this + ") is not in the switch branch");
    }
    return this;

  }

  private TemplatePipelineStep setProcessingType(ProcessingType processingType) {
    this.processingType = processingType;
    return this;
  }

  private TemplatePipelineStep setIsTemplateEmail(Boolean b) {
    this.isTemplateEmail = b;
    return this;
  }

  public TemplatePipelineStep setTargetType(TemplateTargetType templateTargetType) {
    this.templateTargetType = templateTargetType;
    return this;
  }

  @Override
  public TemplatePipelineStep createStepBuilder() {
    return new TemplatePipelineStep();
  }

  @Override
  public TemplatePipelineStepStream build() {

    /**
     * Add the template prefix
     */
    this.setTargetTemplateExtraPrefixes(Set.of(TemplatePipelineStep.TEMPLATE_PREFIX));

    // Mandatory
    if (this.templateSelectors.isEmpty() && this.inlineTemplates.isEmpty()) {
      throw new IllegalArgumentException("A template is mandatory (selector or inline) for the the step (" + this + ")");
    }
    if (this.granularity != Granularity.RECORD && this.templateTargetType == TemplateTargetType.ENRICHED_INPUT) {
      throw new IllegalArgumentException("An enriched input target type should have a record granularity");
    }

    this.targetColumnNameTemplateFunction = TemplateString
      .builder(this.targetColumnName)
      .setExtraPrefixes(TEMPLATE_PREFIX)
      .setPipeline(this.getPipeline())
      .build();


    return new TemplatePipelineStepStream(this);
  }

  @Override
  public KeyNormalizer getOperationName() {
    return TEMPLATE;
  }

  public TemplatePipelineStep setOutput(StepOutputArgument stepOutputArgument) {
    this.output = stepOutputArgument;
    return this;
  }


  public PipelineStepBuilder setEnrichedColumnName(String enrichedColumnName) {
    this.targetColumnName = enrichedColumnName;
    return this;
  }

  public TemplatePipelineStep setGranularity(Granularity granularity) {
    this.granularity = granularity;
    return this;
  }

}
