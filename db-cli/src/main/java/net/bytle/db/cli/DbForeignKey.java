package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.DbLoggers;

import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.COUNT_COMMAND;
import static net.bytle.db.cli.Words.LIST_COMMAND;

public class DbForeignKey {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_CLI;


    public static void run(CliCommand cliCommand, String[] args) {


        cliCommand.commandOf(LIST_COMMAND)
                .setDescription("list the links between tables (foreign keys)");
        cliCommand.commandOf(COUNT_COMMAND)
                .setDescription("count the total number of links");


        CliParser cliParser = Clis.getParser(cliCommand, args);

        List<CliCommand> commands = cliParser.getChildCommands();
        if (commands.size() > 0) {
            for (CliCommand command : commands) {
                switch (command.getName()) {
                    case LIST_COMMAND:
                        DbForeignKeyList.run(command, args);
                        break;
                    case COUNT_COMMAND:
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
