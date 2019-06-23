package net.bytle.doctest;

import net.bytle.cli.CliLog;

import java.util.logging.Logger;

public class DocTestLogger {

    public static final String format = CliLog.DEFAULT_FORMAT;

    public static final Logger LOGGER_DOCTEST =
            CliLog
                    .getCliLog("doctest")
                    .setFormat(format)
                    .setNameSpace("net.bytle.doctest")
                    .getLogger();
}
