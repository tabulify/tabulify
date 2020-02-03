package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.log.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Db {


    // TODO: The files in `doc\files` must be in the Bytle-Db Delivery artifact to be able to follow the tutorials

    public static final Log LOGGER_DB_CLI =  Log.getLog(Db.class);


    /**
     * The database name is an identifier
     * It's used by:
     * * every appHome command
     * * every test command
     * in order to work in the same database space
     * If the test and the command does not use the same database name
     * the test will not succeed as the tables will be recreated
     */
    public static final String CLI_DATABASE_NAME_TARGET = "target";
    public static final String CLI_DATABASE_NAME_SOURCE = "source";


    public static final String TPCDS_DATASTORE = "tpcds";

    // To store  the data
    public static List<Map<String, String>> records = new ArrayList<>();


    public static void main(String[] args) {

      // TODO: TpcDs (The default data store vault should bre created here
      // if we want to add a TPCDS store because tpcds is a plugin
      // DataStore tpcDs = Database.of(TPCDS_DATASTORE)
      //    .setConnectionString(TPCDS_DATASTORE)
      //    .addProperty("scale","0.01");
      // dataStores.put(tpcDs.getName(), tpcDs);

        // A client command example
        String example = "To load data from a csv file you would type:\n" +
                Words.CLI_NAME + " " + Words.TABLE_COMMAND + " " + Words.LOAD_COMMAND + " TABLE_NAME.csv \n";

        // Initiate the client helper
        CliCommand cli = Clis.getCli(Words.CLI_NAME)
                .setDescription("A command line utility tool for every database")
                .addExample(example)
                .setHelpWord(Words.HELP);

        cli.optionOf(Words.CONFIG_FILE_PATH)
                .setDescription("The path to the config file")
                .setEnvName("DB_CONFIG_FILE")
                .setValueName("path")
                .setSystemPropertyName("DB_CONFIG_FILE");
        cli.setConfigWord(Words.CONFIG_FILE_PATH);

        cli.optionOf(Words.HELP)
                .setTypeAsFlag()
                .setShortName("h")
                .setDescription("Print this help");
        cli.setHelpWord(Words.HELP);


        cli.commandOf(Words.TABLE_COMMAND)
                .setDescription("operations on one or several relational tables");
        cli.commandOf(Words.DATASTORE_COMMAND)
                .setDescription("operations on datastore");
        cli.commandOf(Words.SCHEMA_COMMAND)
                .setDescription("operations on a schema");
        cli.commandOf(Words.QUERY_COMMAND)
                .setDescription("operations on a query");
        cli.commandOf(Words.SAMPLE_COMMAND)
                .setDescription("create and fill data into a sample schema");
        cli.commandOf(Words.FKEY_COMMAND)
                .setDescription("operations on foreign key (ie on relation and constraints between tables)");


        Words.initGlobalOptions(cli);

        CliParser cliParser = Clis.getParser(cli, args);

        List<CliCommand> cliCommands = cliParser.getChildCommands();
        if (cliCommands.size() == 0) {
            LOGGER_DB_CLI.severe("A known command must be given as first argument.");
            CliUsage.print(cli);
            System.exit(1);
        }

        CliCommand firstCommand = cliCommands.get(0);
        switch (firstCommand.getName()) {
            case Words.DATASTORE_COMMAND:
                DbDatastore.run(firstCommand, args);
                break;
            case Words.TABLE_COMMAND:
                DbTable.run(firstCommand, args);
                break;
            case Words.SCHEMA_COMMAND:
                DbSchema.run(firstCommand, args);
                break;
            case Words.QUERY_COMMAND:
                DbQuery.run(firstCommand, args);
                break;
            case Words.FKEY_COMMAND:
                DbForeignKey.run(firstCommand, args);
                break;
            default:
                System.err.println("The command (" + firstCommand + ") is unknown or not yet implemented.");
                CliUsage.print(cli);
                System.exit(1);
                break;
        }


    }


}

