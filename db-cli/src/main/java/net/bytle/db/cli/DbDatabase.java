package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.fs.Fs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static net.bytle.db.cli.Words.*;


public class DbDatabase {


    private static final Log LOGGER = Db.LOGGER_DB_CLI;

    // Options used in all sub actions
    static final String STORAGE_PATH = "store";
    static final String BYTLE_DB_DATABASES_STORE = "BYTLE_DB_DATABASES_STORE";
    static final Path DEFAULT_STORAGE_PATH = Paths.get(Fs.getAppData(Words.CLI_NAME).toString(),"databases.ini");

    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.setDescription("Database management");

        cliCommand.commandOf(Words.ADD_COMMAND)
                .setDescription("add a database");
        cliCommand.commandOf(Words.UPSERT_COMMAND)
                .setDescription("update or add a database if it does't exist");
        cliCommand.commandOf(Words.LIST_COMMAND)
                .setDescription("list the databases");
        cliCommand.commandOf(Words.INFO_COMMAND)
                .setDescription("show database information");
        cliCommand.commandOf(Words.REMOVE_COMMAND)
                .setDescription("Remove a database")
                .setAliasName(REMOVE_COMMAND_ALIAS);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        List<CliCommand> commands = cliParser.getChildCommands();
        if (commands.size() > 0) {
            for (CliCommand command : commands) {
                switch (command.getName()) {
                    case ADD_COMMAND:
                        DbDatabaseAdd.run(command, args);
                        break;
                    case UPSERT_COMMAND:
                        DbDatabaseUpsert.run(command, args);
                        break;
                    case LIST_COMMAND:
                        DbDatabaseList.run(command, args);
                        break;
                    case REMOVE_COMMAND:
                        DbDatabaseRemove.run(command, args);
                        break;
                    case INFO_COMMAND:
                        DbDatabaseInfo.run(command, args);
                        break;
                    case SHOW_COMMAND:
                        DbDatabaseInfo.run(command, args);
                        break;
                    default:
                        LOGGER.severe("The command (" + command + ") is unknown");
                        CliUsage.print(cliCommand);
                        System.exit(1);
                }

            }
        } else {
            LOGGER.severe("A known command must be given");
            CliUsage.print(cliCommand);
            System.exit(1);
        }

    }

}
