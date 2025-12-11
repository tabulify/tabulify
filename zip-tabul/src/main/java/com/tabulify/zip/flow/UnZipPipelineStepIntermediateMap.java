package com.tabulify.zip.flow;

import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.flow.operation.StepOutputArgument;
import com.tabulify.fs.FsDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.zip.api.ArchiveEntry;
import com.tabulify.zip.api.ArchiveIterator;
import com.tabulify.zip.datapath.ArchiveDataPath;

import java.nio.file.Path;

public class UnZipPipelineStepIntermediateMap extends PipelineStepIntermediateMapAbs {

  private final UnZipPipelineStep unZipPipelineStep;

  public UnZipPipelineStepIntermediateMap(UnZipPipelineStep unZipPipelineStep) {
    super(unZipPipelineStep);
    this.unZipPipelineStep = unZipPipelineStep;

  }


  @Override
  public DataPath apply(DataPath inputDataPath) {

    ArchiveDataPath archiveDataPath = this.unZipPipelineStep.checkIfArchive(inputDataPath);



    /**
     * Create the results data path
     */
    DataPath results = this.unZipPipelineStep.getResultDataPath(archiveDataPath, null);


    /**
     * Iterate and insert results
     */
    try (
      ArchiveIterator archiveIterator = this.unZipPipelineStep.getIterator(archiveDataPath);
      InsertStream resultsInsertStream = results.getInsertStream();
    ) {

      while (archiveIterator.hasNext()) {

        /**
         * Move the pointer to the next archive entry
         */
        ArchiveEntry entry = archiveIterator.next();

        /**
         * Get the destination
         */
        FsDataPath destinationDataPath = this.unZipPipelineStep.getTargetPath(archiveDataPath, entry);

        /**
         * Copy into the path
         */
        Path absoluteNioPath = destinationDataPath.getAbsoluteNioPath();
        archiveIterator.copyCurrentEntryToPath(absoluteNioPath);

        /**
         * Create the record
         */
        resultsInsertStream.insert(this.unZipPipelineStep.getResultsRecord(entry, destinationDataPath));

      }
    }

    StepOutputArgument outputType = this.unZipPipelineStep.getOutputType();
    switch (outputType) {
      case RESULTS:
        return results;
      case INPUTS:
        return inputDataPath;
      default:
        throw new InternalError("The output type " + outputType + " is unexpected for the map step.");
    }

  }

}
