package net.bytle.doctest;

import net.bytle.log.Log;

import java.util.logging.Logger;

public class DocTestLogger {

    public static final String format = Log.DEFAULT_FORMAT;

    public static final Logger LOGGER_DOCTEST =
            Log
                    .getCliLog("doctest")
                    .setFormat(format)
                    .setNameSpace("net.bytle.doctest")
                    .getLogger();
}
