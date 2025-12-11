package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.operation.*;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUriNode;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;

import java.util.List;
import java.util.stream.Collectors;

import static com.tabulify.tabul.TabulWords.*;
import static com.tabulify.cli.CliUsage.CODE_BLOCK;


public class TabulDataTruncate {

  public static final String FORCE_FLAG = "--" + TruncatePipelineStepArgument.FORCE.toKeyNormalizer().toSqlCase();
  public static final String CASCADE_FLAG = "--" + TruncatePipelineStepArgument.CASCADE.toKeyNormalizer().toSqlCase();

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {


    // Create the command
    childCommand
      .setDescription(
        "Truncate data resources(s) - ie remove all records/content from data resources"
      )
      .addExample(
        "To truncate the tables D_TIME and F_SALES:",
        CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + "D_TIME@connection F_SALES@connection",
        CODE_BLOCK
      )
      .addExample(
        "To truncate only the table D_TIME with force (ie deleting the foreign keys constraint):",
        CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + FORCE_FLAG + "D_TIME@database",
        CODE_BLOCK
      )
      .addExample(
        "To truncate all dimension tables that begins with (D_):",
        CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " D_*@connection",
        CODE_BLOCK
      )
      .addExample(
        "To truncate all tables from the current schema:",
        CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " *@database",
        CODE_BLOCK
      );
    childCommand.addArg(DATA_SELECTORS);

    childCommand.addFlag(FORCE_FLAG)
      .setDescription(DropPipelineStepArgument.FORCE.getDescription())
      .setDefaultValue(false);
    childCommand.addFlag(CASCADE_FLAG)
      .setDescription(DropPipelineStepArgument.CASCADE.getDescription())
      .setDefaultValue(false);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addFlag(NO_STRICT_SELECTION);

    // Args
    CliParser cliParser = childCommand.parse();
    final Boolean withForce = cliParser.getBoolean(FORCE_FLAG);
    final Boolean withCascade = cliParser.getBoolean(CASCADE_FLAG);
    final List<DataUriNode> dataSelectors = cliParser
      .getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toList());
    final Boolean withDependencies = cliParser.getBoolean(TabulWords.WITH_DEPENDENCIES_PROPERTY);
    Boolean isStrictSelection = cliParser.getBoolean(NO_STRICT_SELECTION);

    List<DataPath> dataPaths = Pipeline
      .builder(tabular)
      .addStep(
        SelectPipelineStep.builder()
          .setDataSelectors(dataSelectors)
          .setWithDependencies(withDependencies)
          .setStrictSelection(isStrictSelection)
      )
      .addStep(
        TruncatePipelineStepBatch.builder()
          .setProcessingType(PipelineStepProcessingType.BATCH)
          .setWithForce(withForce)
          .setWithCascade(withCascade)
      )
      .addStep(
        ListPipelineStep.builder()
          .setTargetLogicalName("truncated_data_resource")
          .setTargetDescription("The truncated data resources")
      )
      .build()
      .execute()
      .getDownStreamDataPaths();

    if (dataPaths.isEmpty()) {
      Tabul.printEmptySelectionFeedback(dataSelectors);
    }
    return dataPaths;


  }

}
