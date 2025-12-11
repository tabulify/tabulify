package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.operation.*;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.PrinterPrintFormat;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;
import com.tabulify.exception.CastException;
import com.tabulify.exception.IllegalArgumentExceptions;
import com.tabulify.type.Casts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.tabulify.flow.operation.ExecutePipelineStepArgument.*;
import static com.tabulify.tabul.TabulWords.*;


public class TabulDataExec {


  public static final String STRICT_INPUT_FLAG = "--" + STRICT_INPUT.toKeyNormalizer().toCliLongOptionName();
  public static final String NO_STOP_EARLY_FLAG = "--no-" + STOP_EARLY.toKeyNormalizer().toCliLongOptionName();
  public static final String NO_FAIL_ON_ERROR = "--no-" + FAIL_ON_ERROR.toKeyNormalizer().toCliLongOptionName();
  public static final String EXECUTION_TYPE_OPTION = "--" + EXECUTION_MODE.toKeyNormalizer().toCliLongOptionName();
  public static final String ERROR_DATA_URI_OPTION = "--" + ERROR_DATA_URI.toKeyNormalizer().toCliLongOptionName();
  public static final String TARGET_DATA_URI_OPTION = "--" + ExecutePipelineStepArgument.TARGET_DATA_URI.toKeyNormalizer().toCliLongOptionName();
  public static final String RESULTS_COLUMNS_OPTION = "--" + OUTPUT_RESULT_COLUMNS.toKeyNormalizer().toCliLongOptionName();
  public static final String NO_RUNTIME_RESULTS_PERSISTENCE_FLAG = "--no-" + RUNTIME_RESULT_PERSISTENCE.toKeyNormalizer().toCliLongOptionName();

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    childCommand.setDescription(
      "This command execute one or multiple runtime resources and returns the exit code",
      "",
      "",
      "Note: If you want to:",
      "  * show the execution result, you can use ",
      "     * the `print` command (for the full result)",
      "     * the `head` or `tail` command (for a partial result)",
      "  * transfer/load the result of a runtime (sql query, ...), you should use one of the `transfer` commands (copy, insert, upsert, ...)"
    );
    childCommand.addArg(DATA_SELECTORS)
      .setDescription("One or several runtime data selectors that selects the executable and set the execution connection")
      .setMandatory(true);

    // Examples
    childCommand.addExample(
      "Execute all the queries written in the sql files that begins with `dim` in the current directory (ie `cd` connection)",
      CliUsage.CODE_BLOCK,
      CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " (dim*.sql@cd)@sqlite",
      CliUsage.CODE_BLOCK
    );
    childCommand.addExample(
      "Execute the query file `Query1.sql` against `sqlite` and execute the query file `Query2.sql` against `oracle`",
      CliUsage.CODE_BLOCK,
      CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " (Query1.sql)@sqlite (Query2.sql)@oracle",
      CliUsage.CODE_BLOCK
    );
    String projectWithQueries = "project/withQueries";
    childCommand.addExample(
      "Execute all sql files present in the local directory `" + projectWithQueries + "` against the `postgres` data store and store the result in the `perf` table.",
      CliUsage.CODE_BLOCK,
      CliUsage.TAB + CliUsage.getFullChainOfCommand(childCommand) + " " + OUTPUT_DATA_URI + " perf@postgres (" + projectWithQueries + "/*.sql)@postgres",
      CliUsage.CODE_BLOCK
    );

    // Exc option
    childCommand.addFlag(STRICT_INPUT_FLAG)
      .setDescription(STRICT_INPUT.getDescription())
      .setDefaultValue(!((Boolean) STRICT_INPUT.getDefaultValue()));
    childCommand.addProperty(EXECUTION_TYPE_OPTION)
      .setDescription(EXECUTION_MODE.getDescription())
      .setValueName(Arrays.stream(ExecutionMode.values()).map(Object::toString).collect(Collectors.joining("|")));
    childCommand.addFlag(NO_STOP_EARLY_FLAG)
      .setDescription("If set, the execution will not stop at the first error")
      .setDefaultValue(STOP_EARLY.getDefaultValue());
    childCommand.addFlag(NO_FAIL_ON_ERROR)
      .setDescription("If set, the exit status will be successful even if execution errors are seen")
      .setDefaultValue(FAIL_ON_ERROR.getDefaultValue());
    childCommand.addProperty(ERROR_DATA_URI_OPTION)
      .setDescription(ERROR_DATA_URI.getDescription())
      .setDefaultValue(ERROR_DATA_URI.getDefaultValue().toString());
    childCommand.addProperty(TARGET_DATA_URI_OPTION)
      .setDescription(ExecutePipelineStepArgument.TARGET_DATA_URI.getDescription())
      .setDefaultValue(ExecutePipelineStepArgument.TARGET_DATA_URI.getDefaultValue().toString());
    childCommand.addFlag(NO_RUNTIME_RESULTS_PERSISTENCE_FLAG)
      .setDescription(RUNTIME_RESULT_PERSISTENCE.getDescription())
      .setDefaultValue(RUNTIME_RESULT_PERSISTENCE.getDefaultValue());
    childCommand.addProperty(RESULTS_COLUMNS_OPTION)
      .setDescription(OUTPUT_RESULT_COLUMNS.getDescription());

    // Selection
    childCommand.addFlag(NO_STRICT_SELECTION);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addProperty(PROCESSING_TYPE_OPTION)
      .setDescription("The processing type of the execution operation")
      .setDefaultValue(PipelineStepProcessingType.STREAM.toString());

    // Parse and Args
    CliParser cliParser = childCommand.parse();
    final List<DataUriNode> dataSelectors = cliParser.getStrings(DATA_SELECTORS)
      .stream()
      .map(tabular::createDataUri)
      .collect(Collectors.toList());

    final Boolean withDependencies = cliParser.getBoolean(TabulWords.WITH_DEPENDENCIES_PROPERTY);
    Boolean isStrictSelection = cliParser.getBoolean(NO_STRICT_SELECTION);

    /**
     * Not Strict.
     * Throwing is aberrant and only needed in dev
     * We don't let the choice to the user
     */
    Boolean isStrictExecution = (Boolean) STRICT_EXECUTION.getDefaultValue();

    Boolean runtimeResultPersistence = cliParser.getBoolean(NO_RUNTIME_RESULTS_PERSISTENCE_FLAG);
    Boolean stopEarly = cliParser.getBoolean(NO_STOP_EARLY_FLAG);
    Boolean failOnError = cliParser.getBoolean(NO_FAIL_ON_ERROR);
    Boolean strictInput = cliParser.getBoolean(STRICT_INPUT_FLAG);
    String executionType = cliParser.getString(EXECUTION_TYPE_OPTION);
    ExecutionMode executionModeObject;
    Class<ExecutionMode> executionTypeClass = ExecutionMode.class;
    try {
      executionModeObject = Casts.cast(executionType, executionTypeClass);
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createFromMessageWithPossibleValues(
        "The execution type value (" + executionType + ") is not valid",
        executionTypeClass,
        e);
    }
    String errDataUriString = cliParser.getString(ERROR_DATA_URI_OPTION);
    DataUriStringNode errDataUri;
    try {
      errDataUri = DataUriStringNode.createFromString(errDataUriString);
    } catch (CastException e) {
      throw new IllegalArgumentException("The error data uri value (" + errDataUriString + ") is not valid. Error: " + e.getMessage(), e);
    }

    String processingTypeString = cliParser.getString(PROCESSING_TYPE_OPTION);
    PipelineStepProcessingType processingType;
    try {
      processingType = Casts.cast(processingTypeString,PipelineStepProcessingType.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The processing type value (" + processingTypeString + ") is not valid. Error: " + e.getMessage(), e);
    }
    // We print each record in stream, all records in batch
    PrinterPrintFormat printFormat = PrinterPrintFormat.STREAM;
    if(processingType.equals(PipelineStepProcessingType.BATCH)) {
      printFormat = PrinterPrintFormat.DEFAULT;
    }

    String targetDataUriString = cliParser.getString(TARGET_DATA_URI_OPTION);
    DataUriStringNode targetDataUri;
    try {
      targetDataUri = DataUriStringNode.createFromString(targetDataUriString);
    } catch (CastException e) {
      throw new IllegalArgumentException("The target data uri value (" + targetDataUriString + ") is not valid. Error: " + e.getMessage(), e);
    }

    final List<ExecuteResultAttribute> resultsColumns = new ArrayList<>();
    List<String> strings = cliParser.getStrings(RESULTS_COLUMNS_OPTION);
    for (String resultColumn : strings) {
      try {
        resultsColumns.add(Casts.cast(resultColumn, ExecuteResultAttribute.class));
      } catch (CastException e) {
        throw IllegalArgumentExceptions.createFromMessageWithPossibleValues("The result column value (" + resultColumn + ") is not valid", ExecuteResultAttribute.class, e);
      }
    }

    // Pipeline name is used in the template uri
    // We set it to the command to not see anonymous
    String pipelineNodeName = "tabul-data-exec";

    /**
     * Pipeline
     */
    List<DataPath> dataPaths = Pipeline.builder(tabular)
      .setNodeName(pipelineNodeName)
      .addStep(
        SelectPipelineStep
          .builder()
          .setDataSelectors(dataSelectors)
          .setWithDependencies(withDependencies)
          .setStrictSelection(isStrictSelection)
          .setOrder(SelectPipelineStepArgumentOrder.NATURAL)
      )
      .addStep(
        /**
         * The execution is executed in a stream format
         * so that we get the result right away
         */
        ExecutePipelineStep.builder()
          .setOutputType(StepOutputArgument.RESULTS)
          .setProcessingType(processingType)
          .setExecutionMode(executionModeObject)
          .setStrictExecution(isStrictExecution)
          .setStopEarly(stopEarly)
          .setErrDataUri(errDataUri)
          .setFailOnError(failOnError)
          .setResultsColumns(resultsColumns)
          .setRuntimeResultPersistence(runtimeResultPersistence)
          .setStrictInput(strictInput)
          .setTargetDataUri(tabular.createDataUri(targetDataUri))
      )
      .addStep(
        PrintPipelineStep.builder()
          .setFormat(printFormat)
      )
      .addStep(
        /**
         * We collect each record back
         */
        TransferPipelineStep.builder()
          .setTargetDataUri(
            DataUriNode
              .builder()
              .setPath("execution_result")
              .setConnection(tabular.getMemoryConnection())
              .build()
          )
          .setTransferPropertiesSystem(
            TransferPropertiesSystem.builder()
              .setOperation(TransferOperation.INSERT)
          )
      )
      .build()
      .execute()
      .getDownStreamDataPaths();

    if (dataPaths.isEmpty()) {
      Tabul.printEmptySelectionFeedback(dataSelectors);
    }
    /**
     * Hack to not print twice
     */
    return Tabul.hackToNotPrintTwice(cliParser, dataPaths);

  }
}
