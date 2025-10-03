package com.tabulify.gen.flow.generate;

import com.tabulify.flow.Granularity;
import com.tabulify.flow.engine.PipelineStepRootStreamSupplierAbs;
import com.tabulify.gen.GenDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import net.bytle.type.KeyNormalizer;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * A stream of data generated
 */
public class GeneratePipelineStepStream extends PipelineStepRootStreamSupplierAbs {

  public static final KeyNormalizer GENERATE = KeyNormalizer.createSafe("GENERATE");
  private final GeneratePipelineStep generateBuilder;
  private final ArrayDeque<DataPath> dataPathQueue = new ArrayDeque<>();
  private Map<DataPath, SelectStream> streams = new HashMap<>();

  public GeneratePipelineStepStream(GeneratePipelineStep stepBuilder) {

    super(stepBuilder);
    this.generateBuilder = stepBuilder;
  }

  static public GeneratePipelineStep builder() {
    return new GeneratePipelineStep();
  }


  @Override
  public void onStart() {

    // Generate the stream
    for (GenDataPath genDataPath : generateBuilder.getGenDataPaths()) {
      //noinspection resource
      generateNewStream(genDataPath);
    }

  }

  @Override
  public boolean hasNext() {

    return !this.dataPathQueue.isEmpty();
  }

  @Override
  public void onComplete() {
    streams.values().forEach(SelectStream::close);
  }

  /**
   * Generate the data paths to push
   */
  private void generateDataPaths() {
    for (GenDataPath genDataPath : generateBuilder.getGenDataPaths()) {
      DataPath pushedDataPath;
      Granularity streamGranularity = generateBuilder.getStreamGranularity();
      switch (streamGranularity) {
        case RECORD:
          pushedDataPath = this.generateRecord(genDataPath);
          break;
        case RESOURCE:
          pushedDataPath = genDataPath;
          break;
        default:
          throw new RuntimeException("The granularity " + streamGranularity + " was not processed");
      }
      this.dataPathQueue.add(pushedDataPath);
    }
  }

  private DataPath generateRecord(GenDataPath genDataPath) {
    DataPath generatedDataPath = this.getTabular()
      .getMemoryConnection()
      .getAndCreateRandomDataPath(genDataPath.getLogicalName())
      .mergeDataDefinitionFrom(genDataPath)
      .setLogicalName(genDataPath.getLogicalName());
    try (
      InsertStream insertStream = generatedDataPath.getInsertStream();
    ) {
      SelectStream selectStream = this.streams.get(genDataPath);
      Long streamRecordCount = genDataPath.getStreamRecordCount();
      if (streamRecordCount == null) {
        streamRecordCount = generateBuilder.getStreamRecordCount();
      }
      for (int i = 1; i <= streamRecordCount; i++) {
        boolean hasNext = selectStream.next();
        if (!hasNext) {

          selectStream = generateNewStream(genDataPath);
          selectStream.next();

        }
        insertStream.insert(selectStream.getObjects());
      }
    }
    return generatedDataPath;
  }

  private SelectStream generateNewStream(GenDataPath genDataPath) {
    try {
      SelectStream selectStream = genDataPath.getSelectStream();
      this.streams.put(genDataPath, selectStream);
      return selectStream;
    } catch (SelectException e) {
      throw new RuntimeException("We were unable to get the record stream for the data path " + genDataPath + ". Error: " + e.getMessage(), e);
    }

  }


  @Override
  public DataPath get() {
    return this.dataPathQueue.poll();
  }

  @Override
  public void poll() {
    this.generateDataPaths();
  }

}
