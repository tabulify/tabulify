package com.tabulify.tabli;

import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.step.SelectSupplier;
import com.tabulify.flow.step.TransferOutputArgument;
import com.tabulify.flow.step.TransferStep;
import com.tabulify.spi.DataPath;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.transfer.TransferProperties;
import com.tabulify.uri.DataUri;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.template.flow.TemplateAttributes;
import net.bytle.template.flow.TemplateEngine;
import net.bytle.template.flow.TemplateStep;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tabulify.tabli.TabliWords.*;
import static net.bytle.template.flow.TemplateAttributes.TEMPLATE_ENGINE;


public class TabliDataTemplate {

  public final static String TEMPLATE_SELECTOR_PROPERTY = "--" + KeyNormalizer.createSafe(TemplateAttributes.TEMPLATE_SELECTORS).toCliLongOptionName();
  public static final String TEMPLATE_ENGINE_PROPERTY = "--" + KeyNormalizer.createSafe(TemplateAttributes.TEMPLATE_ENGINE).toCliLongOptionName();


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Command
    childCommand
      .setDescription(
        "Create data resources from a template and the values of the source data resource."
      );

    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addProperty(TabliWords.TABLI_ATTRIBUTE_OPTION)
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


    TabliDataTransferManager.addAllTransferOptions(childCommand, TransferOperation.INSERT);

    // Args
    CliParser cliParser = childCommand.parse();

    TransferProperties transferProperties = TabliDataTransferManager.getTransferProperties(tabular, cliParser);

    final DataUri sourceSelector = tabular.createDataUri(cliParser.getString(SOURCE_SELECTOR));
    final DataUri targetUri = tabular.createDataUri(cliParser.getString(TARGET_DATA_URI));

    String templateEngineValue = cliParser.getString(TEMPLATE_ENGINE_PROPERTY);
    final TemplateEngine templateEngine;
    try {
      templateEngine = Casts.cast(templateEngineValue, TemplateEngine.class);
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createForArgumentValue(templateEngineValue, TEMPLATE_ENGINE, TemplateEngine.class, e);
    }

    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);
    Map<String, ?> attributes = cliParser.getProperties(TabliWords.TABLI_ATTRIBUTE_OPTION);
    Set<DataUri> templateSelectors = cliParser
      .getStrings(TEMPLATE_SELECTOR_PROPERTY)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toSet());
    return Pipeline
      .createFrom(tabular)
      .addStepToGraph(
        SelectSupplier.create()
          .setDataSelector(sourceSelector)
          .setWithDependencies(withDependencies)
          .setAttributes(attributes)
      )
      .addStepToGraph(
        TemplateStep.create()
          .addTemplateSelectors(templateSelectors)
          .setTemplateEngine(templateEngine)
      )
      .addStepToGraph(
        TransferStep.create()
          .setTransferProperties(transferProperties)
          .setTargetUri(targetUri)
          .setOutput(TransferOutputArgument.RESULTS)
      )
      .execute()
      .getDownStreamDataPaths()
      .stream()
      .sorted()
      .collect(Collectors.toList());

  }


}
