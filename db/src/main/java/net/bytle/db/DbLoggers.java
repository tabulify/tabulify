package net.bytle.db;

import net.bytle.log.Log;

import java.util.logging.Logger;


public class DbLoggers {

    public static final String format = Log.DEFAULT_FORMAT;

    public static final Logger LOGGER_DB_GENERATOR =
            Log
                    .getCliLog("generator")
                    .setFormat(format)
                    .setNameSpace("net.bytle.db.generator")
                    .getLogger();

    public static final Logger LOGGER_DB_CLI =
            Log
                    .getCliLog("cli")
                    .setFormat(format)
                    .setNameSpace("net.bytle.db.cli.main")
                    .getLogger();

    public static final Logger LOGGER_DB_DOWNLOADER =
            Log
                    .getCliLog("downloader")
                    .setFormat(format)
                    .setNameSpace("net.bytle.db.downloader")
                    .getLogger();

    public static final Logger LOGGER_DB_ENGINE =
            Log
                    .getCliLog("engine")
                    .setFormat(format)
                    .setNameSpace("net.bytle.db.engine")
                    .getLogger();

    public static final Logger LOGGER_DB_LOADER =
            Log
                    .getCliLog("loader")
                    .setFormat(format)
                    .setNameSpace("net.bytle.db.loader")
                    .getLogger();


    public static final Logger LOGGER_DB_QUERY =
            Log
                    .getCliLog("query")
                    .setFormat(format)
                    .setNameSpace("net.bytle.db.query")
                    .getLogger();

    public static final Logger LOGGER_DB_SAMPLE = Log
            .getCliLog("tpcds")
            .setFormat(format)
            .setNameSpace("net.bytle.db.tpcds")
            .getLogger();

    public static final Logger LOGGER_DB_TEST = Log
            .getCliLog("test")
            .setFormat(format)
            .setNameSpace("net.bytle.test")
            .getLogger();
}
