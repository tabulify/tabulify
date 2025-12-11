package com.tabulify.resource;

import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaTypeAbs;

public class ManifestKindMediaType extends MediaTypeAbs {

  public static final String MANIFEST_SEPARATION = "--";
  private final KeyNormalizer kind;

  public ManifestKindMediaType(KeyNormalizer kind) {
    this.kind = kind;
  }

  @Override
  public String getSubType() {
    return "vnd.tabulify." + getKind() + "+yaml";
  }

  @Override
  public String getType() {
    return "text";
  }

  @Override
  public String getExtension() {
    return MANIFEST_SEPARATION + getKind() + ".yml";
  }

  @Override
  public KeyNormalizer getKind() {
    return this.kind;
  }

}
