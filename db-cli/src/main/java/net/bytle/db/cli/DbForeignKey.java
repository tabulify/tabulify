package net.bytle.db.cli;

import net.bytle.cli.*;

import java.util.List;

import static net.bytle.db.cli.Words.COUNT;
import static net.bytle.db.cli.Words.NO_COUNT;
import static net.bytle.db.cli.Words.LIST_COMMAND;

/**
 * TODO: rename to link because we are showing the foreign keys to or from a table
 */
public class DbForeignKey {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;


    public static void run(CliCommand cliCommand, String[] args) {


        cliCommand.commandOf(LIST_COMMAND)
                .setDescription("list the links between tables (foreign keys)");
        cliCommand.commandOf(COUNT)
                .setDescription("count the total number of links (foreign keys)");


        CliParser cliParser = Clis.getParser(cliCommand, args);

        List<CliCommand> commands = cliParser.getChildCommands();
        if (commands.size() > 0) {
            for (CliCommand command : commands) {
                switch (command.getName()) {
                    case LIST_COMMAND:
                        DbForeignKeyList.run(command, args);
                        break;
                    case COUNT:
                        DbForeignKeyCount.run(command, args);
                        break;
                    default:
                        LOGGER.severe("The command (" + command + ") is unknown.");
                        CliUsage.print(cliCommand);
                        System.exit(1);
                }
            }
        } else {
            LOGGER.severe("A known command must be given.");
            CliUsage.print(cliCommand);
            System.exit(1);
        }

    }


}
