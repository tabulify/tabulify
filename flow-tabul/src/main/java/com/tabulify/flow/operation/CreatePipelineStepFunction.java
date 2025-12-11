package com.tabulify.flow.operation;

import com.tabulify.DbLoggers;
import com.tabulify.engine.ForeignKeyDag;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.StrictException;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CreatePipelineStepFunction implements Function<List<DataPath>, List<DataPath>> {

  private final CreatePipelineStep builder;

  public CreatePipelineStepFunction(CreatePipelineStep step) {
    this.builder = step;
  }

  public DataPath apply(DataPath source) {
    return apply(List.of(source)).get(0);
  }

  @Override
  public List<DataPath> apply(List<DataPath> sources) {

    Map<DataPath, DataPath> sourceTargeMap;
    if (this.builder.getTargetUri() == null) {
      Tabulars.create(sources);
      sourceTargeMap = sources
        .stream()
        .collect(Collectors.toMap(
          s -> s,
          s -> s
        ));
    } else {
      sourceTargeMap = createTarget(sources);
    }

    switch (this.builder.getOutput()) {
      case INPUTS:
        return sources;
      case TARGETS:
        return new ArrayList<>(sourceTargeMap.values());
      case RESULTS:
      default:
        return List.of(toResultDataPath(sourceTargeMap));
    }

  }

  /**
   * source target data path
   */
  private DataPath toResultDataPath(Map<DataPath, DataPath> target) {
    try (InsertStream insertStream = this.builder.getTabular().getMemoryConnection().getAndCreateRandomDataPath()
      .setLogicalName("create_input_target")
      .setComment("Results of the create operation")
      .createRelationDef()
      .addColumn("input")
      .addColumn("target")
      .getDataPath()
      .getInsertStream()) {
      target.forEach((key, value) -> insertStream.insert(key.toDataUri().toString(), value.toDataUri().toString()));
      return insertStream.getDataPath();
    }
  }

  private Map<DataPath, DataPath> createTarget(List<DataPath> sources) {

    if (sources.isEmpty()) {
      if (this.builder.getPipeline().isStrict()) {
        throw new StrictException("The create step didn't get any sources. We can't create any data resources.");
      }
      return new HashMap<>();
    }

    Map<DataPath, DataPath> sourceTargets = sources.stream()
      .collect(Collectors.toMap(
        s -> s,
        builder::getTargetFromInput
      ));

    /**
     * Doing the work
     */
    List<DataPath> dataPathsToCreate;
    if (sources.size() != 1) {
      dataPathsToCreate = ForeignKeyDag.createFromPaths(sources).getCreateOrdered();
    } else {
      /**
       * We allow creating a view that has a query with tables that does not exist
       * for that, no dag as the view metadata cannot be retrieved
       */
      dataPathsToCreate = new ArrayList<>(sources);
    }
    for (DataPath source : dataPathsToCreate) {

      DataPath target = sourceTargets.get(source);

      /**
       * Creation
       */
      if (Tabulars.exists(target)) {
        throw new IllegalArgumentException("The data resources (" + target.getName() + ") exists already in the connection (" + target.getConnection().getName() + ") and was not created");
      }

      /**
       * Let the data system handle it
       * Sql System can create view, table from sql text file
       */
      target.getConnection().getDataSystem().create(target, source, sourceTargets);


      DbLoggers.LOGGER_DB_ENGINE.fine("The data resource (" + target + ") was created");

    }

    return sourceTargets;

  }


}
