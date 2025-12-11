package com.tabulify.gen;

import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypeAbs;

public class GeneratorMediaType {

  static final String GENERATOR_NAME = "generator";
  public static final KeyNormalizer KIND = KeyNormalizer.createSafe(GENERATOR_NAME);
  public static final String FILE_NAME_SUFFIX = "--" + GENERATOR_NAME + ".yml";

  final static public MediaType FS_GENERATOR_TYPE = new MediaTypeAbs() {

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
    public KeyNormalizer getKind() {
      return KIND;
    }

    @Override
    public String getExtension() {
      return FILE_NAME_SUFFIX;
    }

  };

  /**
   * In a `define` pipeline step
   */
  final static public MediaType FS_INLINE_GENERATOR_TYPE = new MediaTypeAbs() {

    public static final String TYPE = "text";
    public static final String SUBTYPE = "vnd.tabulify." + GENERATOR_NAME + "+yaml-fragment";

    @Override
    public String getSubType() {
      return SUBTYPE;
    }

    @Override
    public String getType() {
      return TYPE;
    }

    @Override
    public KeyNormalizer getKind() {
      return KIND;
    }

    @Override
    public String getExtension() {
      return FILE_NAME_SUFFIX;
    }

  };

}
