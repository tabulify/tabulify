package net.bytle.db;

import net.bytle.cli.CliLog;

import java.util.logging.Logger;


public class DbLoggers {

    public static final String format = CliLog.DEFAULT_FORMAT;

    public static final Logger LOGGER_DB_GENERATOR =
            CliLog
                    .getCliLog("generator")
                    .setFormat(format)
                    .setNameSpace("net.bytle.db.generator")
                    .getLogger();

    public static final Logger LOGGER_DB_CLI =
            CliLog
                    .getCliLog("cli")
                    .setFormat(format)
                    .setNameSpace("net.bytle.db.cli.main")
                    .getLogger();

    public static final Logger LOGGER_DB_DOWNLOADER =
            CliLog
                    .getCliLog("downloader")
                    .setFormat(format)
                    .setNameSpace("net.bytle.db.downloader")
                    .getLogger();

    public static final Logger LOGGER_DB_ENGINE =
            CliLog
                    .getCliLog("engine")
                    .setFormat(format)
                    .setNameSpace("net.bytle.db.engine")
                    .getLogger();

    public static final Logger LOGGER_DB_LOADER =
            CliLog
                    .getCliLog("loader")
                    .setFormat(format)
                    .setNameSpace("net.bytle.db.loader")
                    .getLogger();


    public static final Logger LOGGER_DB_QUERY =
            CliLog
                    .getCliLog("query")
                    .setFormat(format)
                    .setNameSpace("net.bytle.db.query")
                    .getLogger();

    public static final Logger LOGGER_DB_SAMPLE = CliLog
            .getCliLog("tpcds")
            .setFormat(format)
            .setNameSpace("net.bytle.db.tpcds")
            .getLogger();

    public static final Logger LOGGER_DB_TEST = CliLog
            .getCliLog("test")
            .setFormat(format)
            .setNameSpace("net.bytle.test")
            .getLogger();
}
