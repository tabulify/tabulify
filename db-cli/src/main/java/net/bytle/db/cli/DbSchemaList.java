package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.model.SchemaDef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.JDBC_DRIVER_TARGET_OPTION;
import static net.bytle.db.cli.Words.JDBC_URL_TARGET_OPTION;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 */
public class DbSchemaList {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String ARG_NAME = "schemaName|pattern...";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "List schemas";

        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(ARG_NAME)
                .setDescription("Names of a schema or a glob patterns")
                .setMandatory(true)
                .setDefaultValue("*");

        cliCommand.optionOf(JDBC_URL_TARGET_OPTION);
        cliCommand.optionOf(JDBC_DRIVER_TARGET_OPTION);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        Database database = Databases.get(Db.CLI_DATABASE_NAME_TARGET);

        /**
         * Within a test, the url of the database may have been set
         * Because the option have a sqlite default, this will cause an error
         */
        if (database.getUrl() == null) {
            database.setUrl(cliParser.getString(JDBC_URL_TARGET_OPTION))
                    .setDriver(cliParser.getString(JDBC_DRIVER_TARGET_OPTION));
        }


        List<String> patterns = cliParser.getStrings(ARG_NAME);

        Map<SchemaDef, Integer> schemasInfo = new HashMap<>();
        for (String pattern : patterns) {
            List<SchemaDef> schemas = database.getSchemas(pattern);
            for (SchemaDef schemaDef : schemas) {
                schemasInfo.put(schemaDef, schemaDef.getTables().size());
            }
        }

        if (schemasInfo.size() == 0) {

            System.out.println("No schemas found");

        } else {

            System.out.println("Schema" + CliUsage.TAB + "Number of tables");
            for (SchemaDef schemaDef : schemasInfo.keySet()) {
                System.out.println(schemaDef + CliUsage.TAB + schemasInfo.get(schemaDef));
            }

        }
        LOGGER.info("Bye !");


    }


}
