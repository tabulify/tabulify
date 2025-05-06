package com.tabulify;

import net.bytle.log.Log;
import net.bytle.log.Logs;


public class DbLoggers {

  public static final Log LOGGER_DB_ENGINE = Logs.createFromClazz(DbLoggers.class);

  public static final Log LOGGER_TABULAR_START = Logs.createFromClazz(DbLoggers.class);

}
