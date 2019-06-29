package net.bytle.db;

import net.bytle.cli.Log;


public class DbLoggers {

    public static final String format = Log.DEFAULT_FORMAT;


    public static final Log LOGGER_DB_ENGINE =
            Log
                    .getLog("engine")
                    .setFormat(format)
                    .setNameSpace(DbLoggers.class.getCanonicalName());



    public static final Log LOGGER_DB_QUERY =
            Log
                    .getLog("query")
                    .setFormat(format)
                    .setNameSpace("net.bytle.db.query");


}
