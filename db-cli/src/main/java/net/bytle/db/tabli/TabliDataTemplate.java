package net.bytle.db.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.db.Tabular;
import net.bytle.db.flow.engine.Pipeline;
import net.bytle.db.flow.step.SelectSupplier;
import net.bytle.db.flow.step.TransferOutputArgument;
import net.bytle.db.flow.step.TransferStep;
import net.bytle.db.spi.DataPath;
import net.bytle.db.transfer.TransferOperation;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.db.uri.DataUri;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.template.flow.TemplateAttributes;
import net.bytle.template.flow.TemplateEngine;
import net.bytle.template.flow.TemplateStep;
import net.bytle.type.Casts;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.bytle.db.tabli.TabliWords.*;
import static net.bytle.template.flow.TemplateAttributes.TEMPLATE_ENGINE;


public class TabliDataTemplate {




  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Command
    childCommand
      .setDescription(
        "Create data resources from a template and the values of the source data resource."
      );

    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addProperty(TabliWords.ATTRIBUTE_PROPERTY)
      .setDescription("Set specific data resource attributes")
      .setValueName("attributeName=value");

    String template_options = "Template Options";
    childCommand.addPropertyFromAttribute(TemplateAttributes.TEMPLATE_SELECTORS)
      .setGroup(template_options)
      .setMandatory(true)
      .setValueName("pattern@connection");

    childCommand.addPropertyFromAttribute(TEMPLATE_ENGINE)
      .setGroup(template_options)
      .setValueName("templateEngine");



    TabliDataTransferManager.addAllTransferOptions(childCommand, TransferOperation.INSERT);

    // Args
    CliParser cliParser = childCommand.parse();

    TransferProperties transferProperties = TabliDataTransferManager.getTransferProperties(tabular, cliParser);

    final DataUri sourceSelector = tabular.createDataUri(cliParser.getString(SOURCE_SELECTOR));
    final DataUri targetUri = tabular.createDataUri(cliParser.getString(TARGET_DATA_URI));

    String templateEngineValue = cliParser.getString(TEMPLATE_ENGINE);
    final TemplateEngine templateEngine;
    try {
      templateEngine = Casts.cast(templateEngineValue, TemplateEngine.class);
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createForArgumentValue(templateEngineValue,TEMPLATE_ENGINE,TemplateEngine.class,e);
    }

    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);
    Map<String, ?> attributes = cliParser.getProperties(TabliWords.ATTRIBUTE_PROPERTY);
    Set<DataUri> templateSelectors = cliParser
      .getStrings(TemplateAttributes.TEMPLATE_SELECTORS)
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
