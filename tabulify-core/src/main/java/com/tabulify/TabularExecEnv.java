package com.tabulify;

public enum TabularExecEnv {

  /**
   * A Tabulify app runs in prod
   */
  PROD("The prod environment"),
  /**
   * A dev is developing a Tabulify app
   */
  DEV("The dev environment"),
  /**
   * A dev is developing Tabulify
   */
  IDE("The ide environment"),
  ;

  private final String description;

  TabularExecEnv(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }

}
