package com.tabulify.type;

import java.util.Objects;

/**
 * An abstract class that every {@link MediaType}
 * should extend
 * Why? Because there is the equality/hash function
 * If you create an enum, you need to copy them or use the {@link MediaTypes#equals(MediaType, Object)}
 */
public abstract class MediaTypeAbs implements MediaType {


  @Override
  public String getExtension() {
    return this.getSubType();
  }

  @Override
  public boolean isContainer() {
    return false;
  }

  @Override
  public String toString() {
    return this.getType() + "/" + this.getSubType();
  }

  @Override
  public int hashCode() {
    return Objects.hash(toString());
  }

  @SuppressWarnings("EqualsDoesntCheckParameterClass")
  @Override
  public boolean equals(Object o) {
    return MediaTypes.equals(this, o);
  }

}
