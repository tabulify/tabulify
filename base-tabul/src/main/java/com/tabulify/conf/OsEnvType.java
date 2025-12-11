package com.tabulify.conf;

/**
 * The type of operating system env
 * @deprecated the env name is defined by the user in the attribute value
 */
@Deprecated
public enum OsEnvType {
  /**
   * An env for a connection
   */
  CONNECTION,
  /**
   * An env for a service
   */
  SERVICE,
  /**
   * An env for tabular
   */
  TABULAR
}
