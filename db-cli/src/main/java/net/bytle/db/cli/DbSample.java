package net.bytle.db.cli;


import net.bytle.cli.*;

import java.util.List;

import static net.bytle.db.cli.Words.CREATE_COMMAND;
import static net.bytle.db.cli.Words.DROP_COMMAND;
import static net.bytle.db.cli.Words.FILL_COMMAND;


public class DbSample {

    /**
     * Sample database
     */


    private static final Log LOGGER = Db.LOGGER_DB_CLI;


    public static void run(CliCommand cliCommand, String[] args) {


        cliCommand.commandOf(Words.CREATE_COMMAND)
                .setDescription("create a sample schema");
        cliCommand.commandOf(Words.FILL_COMMAND)
                .setDescription("fill with data a sample schema");
        cliCommand.commandOf(Words.DROP_COMMAND)
                .setDescription("drop the tables of a sample schema");


        CliParser cliParser = Clis.getParser(cliCommand, args);

        List<CliCommand> commands = cliParser.getChildCommands();
        if (commands.size() > 0) {
            for (CliCommand command : commands) {
                switch (command.getName()) {
                    case FILL_COMMAND:
                        DbSampleFill.run(command, args);
                        break;
                    case CREATE_COMMAND:
                        DbSampleCreate.run(command, args);
                        break;
                    case DROP_COMMAND:
                        DbSampleDrop.run(command, args);
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
