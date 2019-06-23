package net.bytle.db.queryExecutor;

import net.bytle.db.DbLoggers;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class Main {
    private final static Logger logger = DbLoggers.LOGGER_DB_QUERY;

    public static void main(final String[] args) throws Exception {


        if (args.length != 1) {
            logger.severe("Please provide the name of the properties file as the first argument");
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
        logger.info(String.format("Total runtime : %d", System.currentTimeMillis() - start));
    }
}
