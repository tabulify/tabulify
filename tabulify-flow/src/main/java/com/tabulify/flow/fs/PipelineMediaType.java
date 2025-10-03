package com.tabulify.flow.fs;

import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypeAbs;

public class PipelineMediaType {

  final static public MediaType PIPELINE = new MediaTypeAbs() {

    @Override
    public String getSubType() {
      return "vnd.tabulify.pipeline+yaml";
    }

    @Override
    public String getType() {
      return "text";
    }

    @Override
    public String getExtension() {
      return "--pipeline.yml";
    }

  };

  public static final KeyNormalizer KIND = KeyNormalizer.createSafe("pipeline");
}
