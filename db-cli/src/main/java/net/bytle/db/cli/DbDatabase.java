package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.DbLoggers;

import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.*;


public class DbDatabase {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;


    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.setDescription("Database management");

        cliCommand.commandOf(Words.ADD_COMMAND)
                .setDescription("add a database");
        cliCommand.commandOf(Words.LIST_COMMAND)
                .setDescription("list the databases");
        cliCommand.commandOf(Words.INFO_COMMAND)
                .setDescription("show database information");

        CliParser cliParser = Clis.getParser(cliCommand, args);

        List<CliCommand> commands = cliParser.getChildCommands();
        if (commands.size() > 0) {
            for (CliCommand command : commands) {
                switch (command.getName()) {
                    case ADD_COMMAND:
                        DbDatabaseAdd.run(command, args);
                        break;
                    case LIST_COMMAND:
                        DbTableList.run(command, args);
                        break;
                    case INFO_COMMAND:
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
