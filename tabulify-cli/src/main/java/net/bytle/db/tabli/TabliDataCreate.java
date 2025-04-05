package net.bytle.db.tabli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.db.Tabular;
import net.bytle.db.flow.engine.Pipeline;
import net.bytle.db.flow.step.*;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.bytle.db.tabli.TabliLog.LOGGER_TABLI;
import static net.bytle.db.tabli.TabliWords.*;

public class TabliDataCreate {


  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    // The command
    childCommand.setDescription("Create a data resource(s) (table, file, ..) from:",
      "  * the metadata of another data resource",
      "  * a script (DDL)",
      "",
      "If the `target uri` is: ",
      "  * not specified, if the `data selector` is:",
      "       * a script selector, the selected resources will be executed",
      "       * otherwise, a data resource will be created (with the `data selector` as `data uri`)",
      "  * specified, the metadata of the selected resources will copied"
    );
    childCommand.addArg(SOURCE_SELECTOR)
      .setDescription("A data selectors that select one or more data resources (Example: `*--datadef.yml@connection`)")
      .setMandatory(true);
    childCommand.addArg(TARGET_DATA_URI)
      .setDescription("A target data Uri that defines the connection and optionally the table name. If the target is not specified, the selected data resource must be scripts and will just run.")
      .setMandatory(false);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY)
      .setDescription("Create also the table dependencies (ie the foreign data resources will also be created).");
    childCommand.addProperty(SOURCE_ATTRIBUTE);
    childCommand.addProperty(TARGET_ATTRIBUTE_PROPERTY);

    // Args
    final CliParser cliParser = childCommand.parse();

    final DataUri dataSelector = tabular.createDataUri(cliParser.getString(SOURCE_SELECTOR));
    final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);
    final Map<String, String> sourceAttributes = cliParser.getProperties(SOURCE_ATTRIBUTE);

    /**
     * The creation
     * If there is a target, this a copy of metadata
     */
    String argTargetUri = cliParser.getString(TARGET_DATA_URI);
    if (argTargetUri != null) {

      final DataUri targetDataUri = tabular.createDataUri(argTargetUri);
      final Map<String, String> targetAttributes = cliParser.getProperties(TARGET_ATTRIBUTE_PROPERTY);

      LOGGER_TABLI.info("Starting create action into (" + targetDataUri + ") from (" + dataSelector + ") " + (withDependencies ? "with" : "without") + " dependencies.");

      return new ArrayList<>(Pipeline
        .createFrom(tabular)
        .addStepToGraph(
          SelectSupplier
            .create()
            .setDataSelector(dataSelector)
            .setWithDependencies(withDependencies)
            .setAttributes(sourceAttributes)
        )
        .addStepToGraph(
          CreateTargetFunction.create()
            .setTargetUri(targetDataUri)
            .setTargetDataDef(targetAttributes)
        )
        .execute()
        .getDownStreamDataPaths());

    } else {

      if (dataSelector.isScriptSelector()) {

        LOGGER_TABLI.info("Starting create action for the script selector (" + dataSelector + ")");
        return new ArrayList<>(Pipeline
          .createFrom(tabular)
          .addStepToGraph(
            SelectSupplier
              .create()
              .setDataSelector(dataSelector)
          )
          .addStepToGraph(ExecuteFunction.create())
          .addStepToGraph(ListCollector.create()
            .setTargetLogicalName("script_executed")
            .setTargetDescription("The scripts executed")
          )
          .execute()
          .getDownStreamDataPaths());


      } else {

        LOGGER_TABLI.info("Starting create action for the data uri (" + dataSelector + ")");
        return new ArrayList<>(Pipeline
          .createFrom(tabular)
          .addStepToGraph(
            DefineStep
              .create()
              .addUri(dataSelector)
          )
          .addStepToGraph(CreateStep.create())
          .addStepToGraph(ListCollector.create()
            .setTargetLogicalName("data_resources_created")
            .setTargetDescription("The data resources created")
          )
          .execute()
          .getDownStreamDataPaths());

      }
    }


  }

}
