package com.tabulify.gen.generator;

public enum RegexpArgument {
  SEED("seed (if set the data is the same betwee run)"),
  EXPRESSION("a regular expression that defines the pattern of the data that should be generated.");

  private final String desc;

  RegexpArgument(String desc) {
    this.desc = desc;
  }

  public String getDesc() {
    return desc;
  }
}
