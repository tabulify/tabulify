package com.tabulify.gen.flow.fill;

import com.tabulify.connection.Connection;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.engine.PipelineStepIntermediateManyToManyAbs;
import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
import com.tabulify.flow.operation.DefinePipelineStep;
import com.tabulify.flow.operation.SelectPipelineStep;
import com.tabulify.gen.DataGenerator;
import com.tabulify.gen.GenDataPath;
import com.tabulify.gen.memory.GenMemDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.uri.DataUriNode;
import net.bytle.type.KeyNormalizer;

import java.util.*;
import java.util.stream.Collectors;

import static com.tabulify.gen.flow.fill.FillPipelineStepArgument.GENERATOR_SELECTORS;
import static java.util.stream.Collectors.toSet;

/**
 * Get the target as sources and
 * * create generator
 * * or send back selected generators
 */
public class FillPipelineStepBatch extends PipelineStepIntermediateManyToManyAbs {

  private final FillPipelineStep fillBuilder;

  /**
   * A map of generator and their logical name
   */
  Map<String, GenDataPath> logicalNameGgenDataPathMap = new HashMap<>();
  private List<DataPath> acceptedTargetDataPaths = new ArrayList<>();

  public FillPipelineStepBatch(FillPipelineStep stepBuilder) {
    super(stepBuilder);
    this.fillBuilder = stepBuilder;
  }

  @Override
  public void onStart() {

    /**
     * Init the map of generators created by the user
     */
    if (this.fillBuilder.generatorSelectorList.isEmpty()) {
      return;
    }
    List<DataPath> downStreamDataPath = Pipeline.builder(getTabular())
      .addStep(
        SelectPipelineStep.builder()
          .setDataSelectors(this.fillBuilder.generatorSelectorList)
          .setWithDependencies(false)
      )
      .build()
      .execute()
      .getDownStreamDataPaths();

    for (DataPath dataPath : downStreamDataPath) {
      if (dataPath instanceof GenDataPath) {
        logicalNameGgenDataPathMap.put(dataPath.getLogicalName(), (GenDataPath) dataPath);
        continue;
      }
      String generatorSelectorsString = this.fillBuilder.generatorSelectorList.stream().map(DataUriNode::toString).collect(Collectors.joining(", "));
      throw new IllegalArgumentException("The selected data path should be a generator. \nThe (" + dataPath + ") selected by the generator selectors (" + generatorSelectorsString + ") is not a generator resource but a " + dataPath.getMediaType());
    }


  }


  @Override
  public void reset() {
    acceptedTargetDataPaths = new ArrayList<>();
  }

  @Override
  public List<DataPath> getDataPathsBuffer() {
    return this.acceptedTargetDataPaths;
  }

  @Override
  public void accept(DataPath dataPath) {
    this.acceptedTargetDataPaths.add(dataPath);
  }

  @Override
  public PipelineStepSupplierDataPath get() {

    Set<Connection> connections = acceptedTargetDataPaths
      .stream()
      .map(DataPath::getConnection)
      .collect(toSet());
    if (connections.size() > 1) {
      throw new RuntimeException("Filling multiple target from different connection are not yet supported. We found the following connections " + connections
        .stream()
        .sorted()
        .map(Connection::getName)
        .map(KeyNormalizer::toString)
        .collect(Collectors.joining(", ")));
    }


    /**
     * Building the data generation object
     */
    DataGenerator dataGenerator = DataGenerator.create(getTabular());
    // Special case with only one target
    boolean monoFillOperation = logicalNameGgenDataPathMap.size() == acceptedTargetDataPaths.size() && acceptedTargetDataPaths.size() == 1;
    if (monoFillOperation) {
      GenDataPath genDataPath = logicalNameGgenDataPathMap.entrySet().iterator().next().getValue();
      dataGenerator.addTransfer(genDataPath, acceptedTargetDataPaths.iterator().next());
    } else {
      // multi fill
      long maxRecordCount = this.fillBuilder.maxRecordCount;
      for (DataPath targetDataPath : acceptedTargetDataPaths) {
        GenDataPath sourceDataPath = logicalNameGgenDataPathMap.get(targetDataPath.getLogicalName());
        if (sourceDataPath != null) {
          if (sourceDataPath.getMaxRecordCount() == null) {
            sourceDataPath.setMaxRecordCount(maxRecordCount);
          }
          dataGenerator.addTransfer(sourceDataPath, targetDataPath);
        } else {
          // no match by name
          if (targetDataPath.getOrCreateRelationDef().getColumnsSize() == 0) {
            StringBuilder s = new StringBuilder();
            s.append("The target to fill (")
              .append(targetDataPath)
              .append(") has no column definitions.");
            if (!logicalNameGgenDataPathMap.isEmpty()) {
              s.append(" We found the following generators (");
              s.append(logicalNameGgenDataPathMap.values()
                .stream()
                .map(dp -> dp.toDataUri().toString())
                .collect(Collectors.joining(","))
              );
              s.append(") but they don't match the target logical name (")
                .append(targetDataPath.getLogicalName())
                .append(")");
            } else {
              if (this.fillBuilder.generatorSelectorList.isEmpty()) {
                s.append(" You should use the (").append(GENERATOR_SELECTORS).append(") arguments to add a generator.");
              } else {
                String generatorSelectorsString = this.fillBuilder.generatorSelectorList.stream().map(DataUriNode::toString).collect(Collectors.joining(", "));
                s.append(" The given generator selectors (")
                  .append(generatorSelectorsString)
                  .append(") does not select any generators");
              }
            }
            throw new IllegalStateException(s.toString());
          }
          dataGenerator.addDummyTransfer(targetDataPath, maxRecordCount);
        }
      }
    }

    Map<DataPath, GenMemDataPath> targetSourceMap = dataGenerator.generateSourceTargetMap().getSourceTarget()
      .entrySet()
      .stream()
      .collect(Collectors.toMap(
        Map.Entry::getValue,
        Map.Entry::getKey
      ));

    /**
     * Conserve the order
     */
    List<GenMemDataPath> genMemDataPaths = new ArrayList<>();
    for (DataPath targetDataPath : acceptedTargetDataPaths) {
      genMemDataPaths.add(targetSourceMap.get(targetDataPath));
    }


    return (PipelineStepSupplierDataPath) DefinePipelineStep
      .builder()
      .addDataPaths(genMemDataPaths)
      .setIntermediateSupplier(this)
      .build();
  }
}
