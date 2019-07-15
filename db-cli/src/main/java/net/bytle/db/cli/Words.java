package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.db.DbDefaultValue;

import static net.bytle.db.cli.DbDatabase.BYTLE_DB_DATABASES_STORE;

/**
 * Created by gerard on 20-06-2017.
 * Define the global options
 *
 */
public class Words {



    // CliName Bytle Db bdb
    static final String CLI_NAME = "db";

    public static final String DB_NAME = "db.name";
    // Config file words
    static final String CONFIG_FILE_PATH = "conf";

    // Module
    static final String DATABASE_COMMAND = "database";
    static final String TABLE_COMMAND = "table";
    static final String SCHEMA_COMMAND = "schema";
    static final String QUERY_COMMAND = "query";
    static final String SAMPLE_COMMAND = "sample";
    static final String FKEY_COMMAND = "fkey";

    // SubCommands level 2 / Action
    // From Local System to remote System
    static final String LOAD_COMMAND = "load";
    // From Remote System to Local System
    static final String DOWNLOAD_COMMAND = "download";
    // Between two remote system ?
    static final String TRANSFER_COMMAND = "transfer";

    static final String DIFF_COMMAND = "diff";
    static final String FILL_COMMAND = "fill";

    static final String EXECUTE_COMMAND = "execute";
    static final String COUNT_COMMAND = "count";

    // Delete command
    static final String DROP_COMMAND = "drop";
    static final String REMOVE_COMMAND = "remove";
    static final String REMOVE_COMMAND_ALIAS = "rm";

    // Initialize DML
    static final String CREATE_COMMAND = "create";
    static final String ADD_COMMAND = "add";
    static final String UPSERT_COMMAND = "upsert";

    // Scalar UI - Show/Info
    static final String SHOW_COMMAND = "show";
    static final String INFO_COMMAND = "info";
    static final String DESCRIBE_COMMAND = "describe";
    // Table UI -
    static final String LIST_COMMAND = "list";



    static final String JDBC_URL_TARGET_OPTION = "db.target.url";
    static final String JDBC_DRIVER_TARGET_OPTION = "db.target.driver";

    static final String HELP = "help";

    static final String TARGET_TABLE_OPTION = "target.table";
    static final String TARGET_SCHEMA_OPTION = "ts";
    static final String SOURCE_SCHEMA_OPTION = "ss";

    static final String TARGET_WORKER_OPTION = "tw";
    static final String BUFFER_SIZE_OPTION = "bs";
    static final String COMMIT_FREQUENCY_OPTION = "tcf";
    static final String TARGET_BATCH_SIZE_OPTION = "tbs";
    static final String TARGET_CONNECTION_SCRIPT_OPTION = "tcs";
    static final String METRICS_PATH_OPTION = "mp";

    static final String JDBC_URL_SOURCE_OPTION = "db.source.url";
    static final String JDBC_DRIVER_SOURCE_OPTION = "sd";
    static final String SOURCE_QUERY_OPTION = "sq";
    static final String SOURCE_FETCH_SIZE_OPTION = "sfs";

    static final String OUTPUT_FILE_PATH = "output";
    static final String FILE_FORMAT = "format";

    static void initGlobalOptions(CliCommand cliCommand) {


        cliCommand.globalWordOf(FILE_FORMAT)
                .setTypeAsOption()
                .setDescription("Define the file format");

        final String default_database_name = "target";
        cliCommand.globalWordOf(DB_NAME)
                .setTypeAsOption()
                .setEnvName("DB_NAME")
                .setMandatory(true)
                .setDefaultValue(default_database_name)
                .setDescription("defines the database name. \nA database connection is defined through its name. To know more about, see the command `" + Words.CLI_NAME + " " + Words.DATABASE_COMMAND + " " + CliParser.PREFIX_LONG_OPTION + Words.HELP + "`");

        cliCommand.globalWordOf(JDBC_URL_TARGET_OPTION)
                .setTypeAsOption()
                .setEnvName("JDBC_URL_TARGET_OPTION")
                .setMandatory(true)
                .setDescription("defines the JDBC database name");

        cliCommand.globalWordOf(JDBC_URL_TARGET_OPTION)
                .setDefaultValue(Db.JDBC_URL_TARGET_DEFAULT);



        cliCommand.globalWordOf(JDBC_DRIVER_TARGET_OPTION)
                .setTypeAsOption()
                .setEnvName("DB_TARGET_DRIVER")
                .setIsInConfigFile(true)
                .setDescription("defines the TARGET driver for the TARGET database (Example: com.sap.db.jdbc.Driver)");


        cliCommand.globalWordOf(TARGET_TABLE_OPTION)
                .setTypeAsOption()
                .setDescription("defines the TARGET table database");

        cliCommand.globalWordOf(TARGET_SCHEMA_OPTION)
                .setTypeAsOption()
                .setDescription("defines the schema of the target table (Default to the connection schema)");

        cliCommand.globalWordOf(SOURCE_SCHEMA_OPTION)
                .setTypeAsOption()
                .setDescription("defines the schema of the source table (Default to the connection schema)")
                .setMandatory(false);


        cliCommand.globalWordOf(TARGET_WORKER_OPTION)
                .setTypeAsOption()
                .setGroup("Load Options")
                .setDescription("defines the TARGET number of thread against the TARGET database")
                .addDefaultValue(1);

        cliCommand.globalWordOf(BUFFER_SIZE_OPTION)
                .setTypeAsOption()
                .setDescription("defines the size of the buffer between SOURCE and TARGET threads");

        cliCommand.globalWordOf(COMMIT_FREQUENCY_OPTION)
                .setTypeAsOption()
                .setDescription("defines the commit frequency against the TARGET table by batch")
                .addDefaultValue(DbDefaultValue.COMMIT_FREQUENCY);

        cliCommand.globalWordOf(TARGET_BATCH_SIZE_OPTION)
                .setTypeAsOption()
                .setDescription("defines the batch size against the TARGET table")
                .addDefaultValue(10000);


        cliCommand.globalWordOf(TARGET_CONNECTION_SCRIPT_OPTION)
                .setTypeAsOption()
                .setDescription("defines a file that contains script that must be run against the target database");

        cliCommand.globalWordOf(METRICS_PATH_OPTION)
                .setTypeAsOption()
                .setDescription("defines the file path that will save the metrics");


        cliCommand.globalWordOf(JDBC_URL_SOURCE_OPTION)
                .setTypeAsOption()
                .setIsInConfigFile(true)
                .setDescription("defines the SOURCE Jdbc connection String for the SOURCE database (Example: jdbc:oracle:thin:scott/tiger@myhost:1521:mysid)");


        cliCommand.globalWordOf(JDBC_DRIVER_SOURCE_OPTION)
                .setTypeAsOption()
                .setDescription("defines the SOURCE driver for the SOURCE database (Example: oracle.jdbc.OracleDriver)");

        cliCommand.globalWordOf(SOURCE_QUERY_OPTION)
                .setTypeAsOption()
                .setDescription("defines the SOURCE query");


        cliCommand.globalWordOf(SOURCE_FETCH_SIZE_OPTION)
                .setTypeAsOption()
                .setDescription("defines the fetch size against the SOURCE database")
                .setDefaultValue(DbDefaultValue.FETCH_SIZE);

        cliCommand.globalWordOf(OUTPUT_FILE_PATH)
                .setTypeAsOption()
                .setShortName("o")
                .setValueName("path")
                .setDescription("defines the path of the output file");

        cliCommand.globalWordOf(DbDatabase.STORAGE_PATH)
                .setDescription("The path where the database information are stored")
                .setDefaultValue(DbDatabase.DEFAULT_STORAGE_PATH)
                .setEnvName(BYTLE_DB_DATABASES_STORE);

    }


}
