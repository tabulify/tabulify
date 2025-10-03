package com.tabulify.flow.fs;

import com.tabulify.fs.FsFileManagerProvider;
import net.bytle.type.MediaType;

public class PipelineFsProvider extends FsFileManagerProvider {


  static final PipelineFsManager pipelineManager = new PipelineFsManager();

  @Override
  public Boolean accept(MediaType mediaType) {

    return mediaType.equals(PipelineMediaType.PIPELINE);

  }

  @Override
  public PipelineFsManager getFsFileManager() {

    return pipelineManager;
  }

}
