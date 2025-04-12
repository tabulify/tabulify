package com.tabulify.noop;

import net.bytle.log.Log;
import net.bytle.log.Logs;

public class NoOpLog {

   public static Log LOGGER  = Logs.createFromClazz(NoOpLog.class);
}
