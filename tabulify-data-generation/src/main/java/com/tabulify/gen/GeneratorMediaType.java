package com.tabulify.gen;

import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypeAbs;

public class GeneratorMediaType {

  static final String GENERATOR_NAME = "generator";
  public static final KeyNormalizer KIND = KeyNormalizer.createSafe(GENERATOR_NAME);
  public static final String FILE_NAME_SUFFIX = "--" + GENERATOR_NAME + ".yml";

  final static public MediaType DATA_GEN = new MediaTypeAbs() {

    public static final String TYPE = "text";
    public static final String SUBTYPE = "vnd.tabulify." + GENERATOR_NAME + "+yaml";

    @Override
    public String getSubType() {
      return SUBTYPE;
    }

    @Override
    public String getType() {
      return TYPE;
    }

    @Override
    public String getExtension() {
      return FILE_NAME_SUFFIX;
    }

  };

}
