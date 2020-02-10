package net.bytle.db.cli;

import net.bytle.cli.CliCommand;

/**
 * Created by gerard on 20-06-2017.
 * Define the global options
 *
 */
public class Words {


    public static final String XPATH = "xpath";
  public static final String CASCADE = "cascade";
  // CliName Bytle Db bdb
    static final String CLI_NAME = "db";

  public static final String ENV_DATASTORE_VAULT_PATH = Words.CLI_NAME + "_DATASTORE_VAULT";

  public static final String DB_NAME = "db.name";
    // Config file words
    static final String CONFIG_FILE_PATH = "conf";

    // Module
    static final String DATASTORE_COMMAND = "database";
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
    static final String COUNT = "count";

    // Delete command
    static final String DROP_COMMAND = "drop";
    static final String TRUNCATE_COMMAND = "truncate";
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

    // First created for the xml cli
    public static final String EXTRACT = "extract";
    public static final String PRINT = "print";
    public static final String CHECK = "check";
    public static final String UPDATE = "update";
    public static final String GET = "of";
    public static final String STRUCTURE = "structure";

    static final String HELP = "help";

    // Options
    public static final String GLOB_PATERN_DATADEF_FILE = "data-def";
    public static final String NOT_STRICT = "no-strict";
    public static final String FORCE = "force";
    static final String NO_COUNT = "no-count";

    // Move options
    static final String TARGET_WORKER_OPTION = "tw";
    static final String BUFFER_SIZE_OPTION = "bs";
    static final String COMMIT_FREQUENCY_OPTION = "tcf";
    static final String TARGET_BATCH_SIZE_OPTION = "tbs";
    static final String TARGET_CONNECTION_SCRIPT_OPTION = "tcs";
    static final String METRICS_DATA_URI_OPTION = "mp";
    static final String SOURCE_FETCH_SIZE_OPTION = "sfs";


    static final String SOURCE_QUERY_OPTION = "sq";


    static final String OUTPUT_DATA_URI = "output";
    static final String FILE_FORMAT = "format";
    // Options used in all sub actions
    static final String DATASTORE_VAULT_PATH = "database-store";


    public static String TARGET_DATA_URI = "target-data-uri";
    public static String SOURCE_DATA_URI = "source-data-uri";

    static void initGlobalOptions(CliCommand cliCommand) {


        cliCommand.globalWordOf(FILE_FORMAT)
                .setTypeAsOption()
                .setDescription("Define the file format");

        cliCommand.globalWordOf(OUTPUT_DATA_URI)
                .setTypeAsOption()
                .setShortName("o")
                .setValueName("path")
                .setDescription("defines the path of the output file");

        cliCommand.globalWordOf(DATASTORE_VAULT_PATH)
                .setTypeAsOption()
                .setDescription("The path where the database information are stored")
                .setDefaultValue(DbDatastore.DEFAULT_STORAGE_PATH)
                .setEnvName(ENV_DATASTORE_VAULT_PATH);

    }


}
