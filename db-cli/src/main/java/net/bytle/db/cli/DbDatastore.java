package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.fs.Fs;
import net.bytle.log.Log;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static net.bytle.db.cli.Words.*;


public class DbDatastore {


    private static final Log LOGGER = Db.LOGGER_DB_CLI;

    static final String BYTLE_DB_datastoreS_STORE = "BYTLE_DB_datastoreS_STORE";
    static final Path DEFAULT_STORAGE_PATH = Paths.get(Fs.getAppData(Words.CLI_NAME).toString(),"datastores.ini");

    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.setDescription("Datastore management");

        cliCommand.commandOf(Words.ADD_COMMAND)
                .setDescription("Add a datastore");
        cliCommand.commandOf(Words.UPSERT_COMMAND)
                .setDescription("Update or add a datastore if it does't exist");
        cliCommand.commandOf(Words.LIST_COMMAND)
                .setDescription("List the datastores");
        cliCommand.commandOf(Words.INFO_COMMAND)
                .setDescription("Show datastore information");
        cliCommand.commandOf(Words.REMOVE_COMMAND)
                .setDescription("Remove a datastore")
                .setAliasName(REMOVE_COMMAND_ALIAS);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        List<CliCommand> commands = cliParser.getChildCommands();
        if (commands.size() > 0) {
            for (CliCommand command : commands) {
                switch (command.getName()) {
                    case ADD_COMMAND:
                        DbDatastoreAdd.run(command, args);
                        break;
                    case UPSERT_COMMAND:
                        DbDatastoreUpsert.run(command, args);
                        break;
                    case LIST_COMMAND:
                        DbDatastoreList.run(command, args);
                        break;
                    case REMOVE_COMMAND:
                        DbDatastoreRemove.run(command, args);
                        break;
                    case INFO_COMMAND:
                        DbDatastoreInfo.run(command, args);
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
