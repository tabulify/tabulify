package net.bytle.db.gen;

import net.bytle.type.MediaType;

public enum GenDataPathType implements MediaType {

  DATA_GEN();


  @Override
  public String getSubType() {
    return "gen";
  }

  @Override
  public String getType() {
    return "relation";
  }

  @Override
  public boolean isContainer() {
    return false;
  }

  @Override
  public String getExtension() {
    return "--datagen.yml";
  }

  @Override
  public String toString() {
    return this.getType()+"/"+this.getSubType();
  }

}
