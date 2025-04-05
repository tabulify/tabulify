package net.bytle.db.spi;

import net.bytle.type.MediaType;

public enum DataPathType implements MediaType {

  SCRIPT("application", "tabli-script");

  private final String type;
  private final String sub;

  DataPathType(String type, String subtype) {
    this.type = type;
    this.sub = subtype;
  }

  @Override
  public String getSubType() {
    return this.sub;
  }

  @Override
  public String getType() {
    return this.type;
  }

  @Override
  public boolean isContainer() {
    return false;
  }


  @Override
  public String getExtension() {
    return this.sub;
  }

  @Override
  public String toString() {
    return type + '/' + sub;
  }

}
