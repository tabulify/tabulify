package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.operation.SelectPipelineStep;
import com.tabulify.flow.operation.StepOutputArgument;
import com.tabulify.flow.operation.TransferPipelineStep;
import com.tabulify.spi.DataPath;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.uri.DataUriNode;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.exception.CastException;
import com.tabulify.exception.IllegalArgumentExceptions;
import com.tabulify.template.flow.TemplateAttribute;
import com.tabulify.template.flow.TemplateEngine;
import com.tabulify.template.flow.TemplatePipelineStep;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tabulify.tabul.TabulWords.*;
import static com.tabulify.template.flow.TemplateAttribute.TEMPLATE_ENGINE;


public class TabulDataTemplate {

  public final static String TEMPLATE_SELECTOR_PROPERTY = "--" + KeyNormalizer.createSafe(TemplateAttribute.TEMPLATE_SELECTORS).toCliLongOptionName();
  public static final String TEMPLATE_ENGINE_PROPERTY = "--" + KeyNormalizer.createSafe(TemplateAttribute.TEMPLATE_ENGINE).toCliLongOptionName();


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Command
    childCommand
      .setDescription(
        "Create data resources from a template and the values of the source data resource."
      );

    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addProperty(TabulWords.ATTRIBUTE_OPTION)
      .setDescription("Set specific data resource attributes")
      .setValueName("attributeName=value");

    String template_options = "Template Options";

    childCommand.addProperty(TEMPLATE_SELECTOR_PROPERTY)
      .setGroup(template_options)
      .setMandatory(true)
      .setValueName("pattern@connection");

    childCommand.addProperty(TEMPLATE_ENGINE_PROPERTY)
      .setGroup(template_options)
      .setValueName("templateEngine");


    /**
     * Template generate a resource and transfer it somewhere
     */
    TabulDataTransferManager tabularTransferManager = TabulDataTransferManager
      .config(tabular, childCommand, TransferOperation.INSERT, TabulDataTransferCommandType.DEFAULT)
      .build();

    // Args
    CliParser cliParser = tabularTransferManager.getParser();


    final DataUriNode sourceSelector = tabular.createDataUri(cliParser.getString(TabulWords.SOURCE_SELECTORS));
    final DataUriNode targetUri = tabular.createDataUri(cliParser.getString(TARGET_DATA_URI));

    String templateEngineValue = cliParser.getString(TEMPLATE_ENGINE_PROPERTY);
    final TemplateEngine templateEngine;
    try {
      templateEngine = Casts.cast(templateEngineValue, TemplateEngine.class);
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createForArgumentValue(templateEngineValue, TEMPLATE_ENGINE, TemplateEngine.class, e);
    }

    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);
    Map<String, ?> attributesRaw = cliParser.getProperties(TabulWords.ATTRIBUTE_OPTION);
    Map<KeyNormalizer, ?> attributes;
    try {
      attributes = Casts.castToNewMap(attributesRaw, KeyNormalizer.class, Object.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The validation of the value of the option " + ATTRIBUTE_OPTION + " returns an error " + e.getMessage(), e);
    }
    Set<DataUriNode> templateSelectors = cliParser
      .getStrings(TEMPLATE_SELECTOR_PROPERTY)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toSet());
    return Pipeline
      .builder(tabular)
      .addStep(
        SelectPipelineStep.builder()
          .setDataSelector(sourceSelector)
          .setWithDependencies(withDependencies)
          .setDataDef(attributes)
      )
      .addStep(
        TemplatePipelineStep.builder()
          .addTemplateSelectors(templateSelectors)
          .setTemplateEngine(templateEngine)
      )
      .addStep(
        TransferPipelineStep.builder()
          .setTransferCrossProperties(tabularTransferManager.getCrossTransferProperties())
          .setTargetDataUri(targetUri)
          .setOutput(StepOutputArgument.RESULTS)
      )
      .build()
      .execute()
      .getDownStreamDataPaths()
      .stream()
      .sorted()
      .collect(Collectors.toList());

  }


}
