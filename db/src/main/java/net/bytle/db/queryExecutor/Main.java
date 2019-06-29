package net.bytle.db.queryExecutor;

import net.bytle.cli.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Main {
    protected final static Log LOGGER_QUERY_EXECUTOR = Log.getLog(Main.class);

    public static void main(final String[] args) throws Exception {


        if (args.length != 1) {
            LOGGER_QUERY_EXECUTOR.severe("Please provide the name of the properties file as the first argument");
            return;
        }

        long start = System.currentTimeMillis();
        new Load()
                .setup(new Properties() {{
                    load(new FileInputStream(new File(args[0])));
                }})
                .run()
                .join()
                .collect();
        LOGGER_QUERY_EXECUTOR.info(String.format("Total runtime : %d", System.currentTimeMillis() - start));
    }
}
