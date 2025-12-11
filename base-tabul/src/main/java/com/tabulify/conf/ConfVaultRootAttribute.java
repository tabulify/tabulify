package com.tabulify.conf;

/**
 * The root attribute of the conf file
 */
public enum ConfVaultRootAttribute {
  ENVS(1),
  CONNECTIONS(2),
  SERVICES(3);

  private final int writeOrder;

  /**
   * @param yamlOrder - the order in the written yaml file
   */
  ConfVaultRootAttribute(int yamlOrder) {
    this.writeOrder = yamlOrder;
  }

  public int getOrder() {
    return writeOrder;
  }
}
