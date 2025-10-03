package com.tabulify.gen.generator;

public enum MetaAttributeArgument {

  DATA_URI("The data path (if null, pipeline input)"),
  ATTRIBUTE("A data resource attribute");

  private final Object desc;

  MetaAttributeArgument(String description) {
    this.desc = description;
  }

  public Object getDesc() {
    return desc;
  }

}
