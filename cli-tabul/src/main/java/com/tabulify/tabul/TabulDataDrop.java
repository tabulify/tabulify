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


public class TabulDataDrop {

  public static final String FORCE_FLAG = "--" + DropPipelineStepArgument.FORCE.toKeyNormalizer().toSqlCase();
  public static final String CASCADE_FLAG = "--" + DropPipelineStepArgument.CASCADE.toKeyNormalizer().toSqlCase();

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // Create the parser
    childCommand
      .setDescription("Drop data resource(s)")
      .addExample("To drop the tables D_TIME and F_SALES in the Oracle connection:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " D_TIME@oracle F_SALES@oracle",
        CliUsage.CODE_BLOCK)
      .addExample("To drop only the table D_TIME with force (ie deleting the foreign keys constraint):",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " " + FORCE_FLAG + " D_TIME@sqlite",
        CliUsage.CODE_BLOCK
      )
      .addExample("To drop all dimension tables that begins with (D_):",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " D_*@sql-server",
        CliUsage.CODE_BLOCK)
      .addExample("To drop all tables from the current schema:",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " *@database",
        CliUsage.CODE_BLOCK
      );
    childCommand.addArg(DATA_SELECTORS);
    childCommand.addFlag(NO_STRICT_SELECTION);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY)
      .setDescription("if set, the foreign table referencing the tables will be dropped");
    childCommand.addFlag(FORCE_FLAG)
      .setDescription("if set, the foreign keys referencing the tables to drop will be dropped");
    childCommand.addFlag(CASCADE_FLAG)
      .setDescription("if set, the dependent resources will be dropped recursively");


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

    /**
     * Natural Order and batch processing to be able to drop bad view
     */
    SelectPipelineStepArgumentOrder naturalOrder = SelectPipelineStepArgumentOrder.NATURAL;
    PipelineStepProcessingType batch = PipelineStepProcessingType.BATCH;
    List<DataPath> dataPaths = Pipeline.builder(tabular)
      .addStep(
        SelectPipelineStep.builder()
          .setDataSelectors(dataSelectors)
          .setWithDependencies(withDependencies)
          .setStrictSelection(isStrictSelection)
          .setOrder(naturalOrder)
      )
      .addStep(
        DropPipelineStep
          .builder()
          .setWithForce(withForce)
          .setWithCascade(withCascade)
          .setProcessingType(batch)
      )
      .addStep(
        ListPipelineStep.builder()
          .setTargetDescription("Dropped data resources")
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
