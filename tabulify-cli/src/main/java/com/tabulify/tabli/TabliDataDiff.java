package com.tabulify.tabli;

import com.tabulify.Tabular;
import com.tabulify.diff.DataComparisonAttribute;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.step.CompareStep;
import com.tabulify.flow.step.CompareStepReportType;
import com.tabulify.flow.step.CompareStepSource;
import com.tabulify.flow.step.SelectSupplier;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUri;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.type.Casts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tabulify.tabli.TabliWords.*;
import static net.bytle.cli.CliUsage.CODE_BLOCK;


/**
 *
 */
public class TabliDataDiff {


  protected static final String UNIQUE_COLUMN_PROPERTY = "--unique-column";

  protected static final String REPORT_LEVEL_PROPERTY = "--report-level";
  protected static final String DATA_SOURCE_PROPERTY = "--data-source";


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // The command
    childCommand
      .setDescription("Performs a comparison (diff) between data resources.",
        "",
        "Prerequisites:",
        "  * The data resources to compare should have been sorted in a ascendant order on the unique columns",
        "  * The data resources to compare should have the same number of columns",
        "",
        "Unique Columns:",
        "It's highly recommended to set the `" + UNIQUE_COLUMN_PROPERTY + "` option.",
        "The `unique columns` are the unique identifier for a record and drives the gathering of the record to compare.",
        "Without unique columns, the comparison can not tell if a record is absent from one set or another.",
        "By default, this is",
        "  * the record id (ie row id, line id) for a data comparison",
        "  * the column name for a data structure comparison",
        "",
        "With the `" + REPORT_LEVEL_PROPERTY + "` option, you can control the output: ",
        "  * `" + CompareStepReportType.RESOURCE + "` will return a data comparison reported resource by resource (default), ",
        "  * `" + CompareStepReportType.RECORD + "` will return a data comparison reported record by record (A diff),",
        "  * `" + CompareStepReportType.ALL + "` will return them both",
        "",
        "With the `" + DATA_SOURCE_PROPERTY + "` option, you can control the compared data: ",
        "  * `" + CompareStepSource.CONTENT + "` will perform a comparison on the content of the data resource, ",
        "  * `" + CompareStepSource.STRUCTURE + "` will perform a comparison on the structure of the data resource (by attribute name) ",
        "  * `" + CompareStepSource.ATTRIBUTE + "` will perform a comparison on the attributes of the data resource (by attribute key) ",
        "",
        "Exit:",
        "If there is a non-equality (ie a diff), the process will exist with an error status.",
        "",
        "Data Comparison Row by Row Report:",
        "The row by row report adds the following columns:",
        "  * `" + DataComparisonAttribute.COMP_ID + "`: a row id of the comparison report",
        "  * `" + DataComparisonAttribute.COMP_ORIGIN + " `: the origin of the record (the source, the target or both)",
        "  * `" + DataComparisonAttribute.COMP_COMMENT + "`: a high level comment that explains the difference",
        "  * `" + DataComparisonAttribute.COMP_DIFF_ID + "`: the id of a difference - not null if a difference has been seen. Two records with the same diff id have been compared",
        "  * `" + DataComparisonAttribute.COMP_ORIGIN_ID + "`: the record id if the comparison can not determine an unique column"
      )
      .addExample("Data comparison between two different queries:",
        CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " (queryFile1.sql)@sqlite (queryFile2.sql)@sqlite",
        CODE_BLOCK
      )
      .addExample("Data comparison between a query and a table on two different systems",
        CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " (queryFile.sql)@sqlite table@postgres",
        CODE_BLOCK
      );
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addProperty(UNIQUE_COLUMN_PROPERTY)
      .setDescription("The unique column name of the data resource (Default to record id for data comparison and to `name` for a structure comparison)")
      .setValueName("columnName");

    childCommand.addArg(SOURCE_SELECTOR)
      .setDescription("A data selector that select the data resources to compare")
      .setMandatory(true);
    childCommand.addArg(TARGET_DATA_URI)
      .setDescription("A target data URI that defines the target to compare - if not defined, an empty target")
      .setMandatory(false);
    childCommand.addProperty(SOURCE_ATTRIBUTE);
    childCommand.addProperty(TARGET_ATTRIBUTE_PROPERTY);


    /**
     * Report
     */
    childCommand.addProperty(REPORT_LEVEL_PROPERTY)
      .setDescription("Set the report level returned")
      .setShortName("-rl")
      .setValueName(Arrays.stream(CompareStepReportType.values()).map(CompareStepReportType::toString).collect(Collectors.joining("|")))
      .setDefaultValue(CompareStepReportType.RESOURCE);

    /**
     * Data Source
     */
    childCommand.addProperty(DATA_SOURCE_PROPERTY)
      .setDescription("Set the origin of data to use for the comparison")
      .setShortName("-ds")
      .setValueName(Arrays.stream(CompareStepSource.values()).map(CompareStepSource::toString).collect(Collectors.joining("|")))
      .setDefaultValue(CompareStepSource.CONTENT.toString());


    // Args
    CliParser cliParser = childCommand.parse();
    final DataUri dataSelectors = tabular.createDataUri(cliParser.getString(SOURCE_SELECTOR));
    String targetUriArg = cliParser.getString(TARGET_DATA_URI);
    DataUri dataUri = null;
    if (targetUriArg != null) {
      dataUri = tabular.createDataUri(targetUriArg);
    }
    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);
    final Map<String, String> sourceAttributes = cliParser.getProperties(SOURCE_ATTRIBUTE);
    final Map<String, String> targetAttributes = cliParser.getProperties(TARGET_ATTRIBUTE_PROPERTY);
    final CompareStepReportType report;
    String reportLevelValue = cliParser.getString(REPORT_LEVEL_PROPERTY);
    try {
      report = Casts.cast(reportLevelValue, CompareStepReportType.class);
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createForArgumentValue(reportLevelValue,REPORT_LEVEL_PROPERTY,CompareStepReportType.class,e);
    }
    String dataSourceValue = cliParser.getString(DATA_SOURCE_PROPERTY);
    final CompareStepSource dataSource;
    try {
      dataSource = Casts.cast(dataSourceValue, CompareStepSource.class);
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createForArgumentValue(dataSourceValue,DATA_SOURCE_PROPERTY,CompareStepReportType.class,e);
    }

    List<String> driverColumns = cliParser.getStrings(UNIQUE_COLUMN_PROPERTY);

    return new ArrayList<>(
      Pipeline
        .createFrom(
          tabular)
        .addStepToGraph(
          SelectSupplier.create()
            .setDataSelector(dataSelectors)
            .setWithDependencies(withDependencies)
            .setAttributes(sourceAttributes)
        )
        .addStepToGraph(
          CompareStep.create()
            .setDriverColumns(driverColumns)
            .setReport(report)
            .setSource(dataSource)
            .setTargetUri(dataUri)
            .setTargetDataDef(targetAttributes)
        )
        .execute()
        .getDownStreamDataPaths()
    );


  }


}
