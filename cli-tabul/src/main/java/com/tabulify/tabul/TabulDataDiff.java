package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.diff.DataDiffEqualityType;
import com.tabulify.diff.DataDiffReportDensity;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.engine.PipelineStepBuilderTargetArgument;
import com.tabulify.flow.operation.*;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUriNode;
import com.tabulify.cli.CliCommand;
import com.tabulify.cli.CliParser;
import com.tabulify.cli.CliUsage;
import com.tabulify.exception.CastException;
import com.tabulify.exception.IllegalArgumentExceptions;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tabulify.tabul.TabulWords.*;
import static com.tabulify.cli.CliUsage.CODE_BLOCK;


/**
 *
 */
public class TabulDataDiff {


  protected static final String DRIVER_COLUMN_PROPERTY = "--" + KeyNormalizer.createSafe(DiffPipelineStepArgument.DRIVER_COLUMNS).toCliLongOptionName();

  protected static final String REPORT_TYPE_PROPERTY = "--" + KeyNormalizer.createSafe(DiffPipelineStepArgument.REPORT_TYPE).toCliLongOptionName();
  protected static final String EQUALITY_TYPE_PROPERTY = "--" + KeyNormalizer.createSafe(DiffPipelineStepArgument.EQUALITY_TYPE).toCliLongOptionName();
  protected static final String MAX_CHANGE_COUNT_PROPERTY = "--" + KeyNormalizer.createSafe(DiffPipelineStepArgument.MAX_CHANGE_COUNT).toCliLongOptionName();
  protected static final String DIFF_DATA_ORIGIN = "--diff-data-origin";
  protected static final String NO_FAIL = "--no-fail";
  protected static final String SPARSE = "--sparse";
  protected static final String NO_COLORS = "--no-colors";


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // The command
    childCommand
      .setDescription("Performs a comparison (diff) between data resources.",
        "",
        "To have a meaningful output, we recommend that the data resources to diff",
        "have been sorted in a ascendant order on the unique columns",
        "",
        "Driver Columns (Unique Columns):",
        "It's highly recommended to set the `" + DRIVER_COLUMN_PROPERTY + "` option.",
        "The `driver columns` are the record identifier and drives the gathering of the record to compare.",
        "Without driver columns, all columns are taken in order.",
        "By default, this is",
        "  * all columns in order for a record diff",
        "  * the column name for a structure diff",
        "",
        "With the `" + REPORT_TYPE_PROPERTY + "` option, you can control the output:",
        "  * `" + DiffPipelineStepReportType.SUMMARY.toString().toLowerCase() + "` will return a summary report,",
        "  * `" + DiffPipelineStepReportType.UNIFIED.toString().toLowerCase() + "` will return a unified report (default, record grain)",
        "",
        "With the `" + DIFF_DATA_ORIGIN + "` option, you can control the origin of data:",
        "  * `" + DiffPipelineStepDataOrigin.RECORD.toString().toLowerCase() + "` will perform a comparison on the records of the data resource,",
        "  * `" + DiffPipelineStepDataOrigin.STRUCTURE.toString().toLowerCase() + "` will perform a comparison on the structure of the data resource (by attribute name)",
        "  * `" + DiffPipelineStepDataOrigin.ATTRIBUTES.toString().toLowerCase() + "` will perform a comparison on the attributes of the data resource (by attribute key)",
        "",
        "Exit:",
        "If there is a non-equality (ie a diff), the process will exist with an error status, unless " + NO_FAIL + " is set."
      )
      .addExample("Data diff between two different queries located in the current directory:",
        CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " (queryFile1.sql)@sqlite (queryFile2.sql)@sqlite",
        CODE_BLOCK
      )
      .addExample("Data diff between a query and a table on two different systems",
        CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " (queryFile.sql)@sqlite table@postgres",
        CODE_BLOCK
      );
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addProperty(DRIVER_COLUMN_PROPERTY)
      .setDescription(DiffPipelineStepArgument.DRIVER_COLUMNS.getDescription())
      .setValueName("columnName");

    childCommand.addArg(TabulWords.SOURCE_SELECTORS)
      .setDescription("A data selector that selects the data resources to compare")
      .setMandatory(true);
    childCommand.addArg(TARGET_DATA_URI)
      .setDescription(PipelineStepBuilderTargetArgument.TARGET_DATA_URI.getDescription())
      .setMandatory(false);
    childCommand.addProperty(SOURCE_ATTRIBUTE);
    childCommand.addProperty(TARGET_ATTRIBUTE);
    childCommand.addFlag(STRICT_SELECTION);
    childCommand.addFlag(NO_FAIL)
      .setDescription(DiffPipelineStepArgument.FAIL.getDescription())
      .setDefaultValue(false);
    childCommand.addFlag(NO_COLORS)
      .setDescription(DiffPipelineStepArgument.TERMINAL_COLORS.getDescription())
      .setDefaultValue(false);
    childCommand.addFlag(SPARSE)
      .setDescription("If set, the unified report output will be sparse (ie only the changes are seen, no context)")
      .setDefaultValue(false);

    /**
     * Max change count
     */
    childCommand.addProperty(MAX_CHANGE_COUNT_PROPERTY)
      .setDescription(DiffPipelineStepArgument.MAX_CHANGE_COUNT.getDescription())
      .setDefaultValue(DiffPipelineStepArgument.MAX_CHANGE_COUNT.getDefaultValue().toString())
      .setMandatory(false);

    /**
     * Equality Type
     */
    childCommand.addProperty(EQUALITY_TYPE_PROPERTY)
      .setShortName("-et")
      .setDescription(DiffPipelineStepArgument.EQUALITY_TYPE.getDescription())
      .setDefaultValue(DiffPipelineStepArgument.EQUALITY_TYPE.getDefaultValue().toString())
      .setValueName(Arrays.stream(DataDiffEqualityType.values())
        .map(DataDiffEqualityType::toString)
        .map(String::toLowerCase)
        .collect(Collectors.joining("|")))
      .setMandatory(false);

    /**
     * Report
     */
    childCommand.addProperty(REPORT_TYPE_PROPERTY)
      .setDescription(DiffPipelineStepArgument.REPORT_TYPE.getDescription())
      .setShortName("-rt")
      .setValueName(
        Arrays.stream(DiffPipelineStepReportType.values())
          .map(DiffPipelineStepReportType::toString)
          .map(String::toLowerCase)
          .collect(Collectors.joining("|")))
      .setDefaultValue(DiffPipelineStepArgument.REPORT_TYPE.getDefaultValue());

    /**
     * The type of diff
     */
    childCommand.addProperty(DIFF_DATA_ORIGIN)
      .setDescription(DiffPipelineStepArgument.DATA_ORIGIN.getDescription())
      .setShortName("-do")
      .setValueName(Arrays.stream(DiffPipelineStepDataOrigin.values())
        .map(DiffPipelineStepDataOrigin::toString)
        .map(String::toLowerCase)
        .collect(Collectors.joining("|")))
      .setDefaultValue(DiffPipelineStepDataOrigin.RECORD.toString());

    // Args
    CliParser cliParser = childCommand.parse();
    final DataUriNode dataSelector = tabular.createDataUri(cliParser.getString(TabulWords.SOURCE_SELECTORS));
    String targetUriArg = cliParser.getString(TARGET_DATA_URI);
    DataUriNode dataUri = null;
    if (targetUriArg != null) {
      dataUri = tabular.createDataUri(targetUriArg);
    }
    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);
    final Boolean sparse = cliParser.getBoolean(SPARSE);
    DataDiffReportDensity dataDiffReportDensity = DataDiffReportDensity.DENSE;
    if (sparse) {
      dataDiffReportDensity = DataDiffReportDensity.SPARSE;
    }
    final Map<String, String> sourceAttributesRaw = cliParser.getProperties(SOURCE_ATTRIBUTE);
    final Map<KeyNormalizer, String> sourceAttributes;
    try {
      sourceAttributes = Casts.castToNewMap(sourceAttributesRaw, KeyNormalizer.class, String.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The validation of the values of the option " + SOURCE_ATTRIBUTE + " returns an error: " + e.getMessage(), e);
    }
    final Map<String, String> targetAttributesRaw = cliParser.getProperties(TARGET_ATTRIBUTE);
    final Map<KeyNormalizer, String> targetAttributes;
    try {
      targetAttributes = Casts.castToNewMap(targetAttributesRaw, KeyNormalizer.class, String.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The validation of the value of the option " + TARGET_ATTRIBUTE + " returns an error " + e.getMessage(), e);
    }

    final DiffPipelineStepReportType report;
    String reportLevelValue = cliParser.getString(REPORT_TYPE_PROPERTY);
    try {
      report = Casts.cast(reportLevelValue, DiffPipelineStepReportType.class);
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createForArgumentValue(reportLevelValue, REPORT_TYPE_PROPERTY, DiffPipelineStepReportType.class, e);
    }
    String dataSourceValue = cliParser.getString(DIFF_DATA_ORIGIN);
    final DiffPipelineStepDataOrigin diffType;
    try {
      diffType = Casts.cast(dataSourceValue, DiffPipelineStepDataOrigin.class);
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createForArgumentValue(dataSourceValue, DIFF_DATA_ORIGIN, DiffPipelineStepReportType.class, e);
    }

    final DataDiffEqualityType equalityType;
    String equalityTypeProperty = cliParser.getString(EQUALITY_TYPE_PROPERTY);
    try {
      equalityType = Casts.cast(equalityTypeProperty, DataDiffEqualityType.class);
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createForArgumentValue(equalityTypeProperty, EQUALITY_TYPE_PROPERTY, DataDiffEqualityType.class, e);
    }

    List<String> driverColumns = cliParser.getStrings(DRIVER_COLUMN_PROPERTY);

    Boolean strict = cliParser.getBoolean(STRICT_SELECTION);
    List<DataPath> dataPaths = Pipeline
      .builder(tabular)
      .addStep(
        SelectPipelineStep.builder()
          .setDataSelector(dataSelector)
          .setWithDependencies(withDependencies)
          .setDataDef(sourceAttributes)
          .setStrictSelection(strict)
      )
      .addStep(
        DiffPipelineStep.builder()
          .setDriverColumns(driverColumns)
          .setFail(!cliParser.getBoolean(NO_FAIL))
          .setTerminalColors(!cliParser.getBoolean(NO_COLORS))
          .setEqualityType(equalityType)
          .setMaxChangeCount(Long.valueOf(cliParser.getInteger(MAX_CHANGE_COUNT_PROPERTY)))
          .setReportDensity(dataDiffReportDensity)
          .setReportType(report)
          .setDataOrigin(diffType)
          .setTargetDataUri(dataUri)
          .setTargetDataDef(targetAttributes)
      )
      .build()
      .execute()
      .getDownStreamDataPaths();

    // Select is not strict by default
    if (!strict && dataPaths.isEmpty()) {
      Tabul.printEmptySelectionFeedback(List.of(dataSelector));
    }

    return dataPaths;
  }


}
