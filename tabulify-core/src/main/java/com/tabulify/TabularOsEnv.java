package com.tabulify;

import static com.tabulify.TabularVariables.TABLI_PREFIX;

/**
 * Env values used in the cli to retrieve values from os env
 * We now also load them at {@link TabularVariables}
 * so we need to merge this 2 ways to load env value in the future
 * (ie not via cli or not via load)
 */
public class TabularOsEnv {

  public static final String TABLI_ENV = TABLI_PREFIX.toUpperCase() + "_" + TabularAttributes.ENV;
  public static final String TABLI_HOME = TABLI_PREFIX.toUpperCase() + "_" + TabularAttributes.HOME;
  public static final String TABLI_CONNECTION_VAULT = TABLI_PREFIX.toUpperCase() + "_" + TabularAttributes.CONNECTION_VAULT;
  public static final String TABLI_PASSPHRASE = TABLI_PREFIX.toUpperCase() + "_" + TabularAttributes.PASSPHRASE;
  public static final String TABLI_PROJECT_HOME = TABLI_PREFIX.toUpperCase() + "_" + TabularAttributes.PROJECT_HOME;
  public static final String TABLI_SQLITE_HOME = TABLI_PREFIX.toUpperCase() + "_" + TabularAttributes.SQLITE_HOME;

}
