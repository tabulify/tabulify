package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.database.Database;
import net.bytle.db.model.SchemaDef;
import net.bytle.log.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * Created by gerard on 08-12-2016.
 * <p>
 */
public class DbSchemaList {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    private static final String ARG_NAME = "schemaNameTest|pattern...";


    public static void run(CliCommand cliCommand, String[] args) {

        String description = "List schemas";

        // Create the parser
        cliCommand
                .setDescription(description);

        cliCommand.argOf(ARG_NAME)
                .setDescription("Names of a schema or a glob patterns")
                .setMandatory(true)
                .setDefaultValue("*");

        CliParser cliParser = Clis.getParser(cliCommand, args);

        Database database = Databases.of(Db.CLI_DATABASE_NAME_TARGET);


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
