package net.bytle.db.cli;


import net.bytle.cli.*;

import java.util.List;

import static net.bytle.db.cli.Words.FILL_COMMAND;
import static net.bytle.db.cli.Words.LIST_COMMAND;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 */
public class DbSchema {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;


    public static void run(CliCommand cliCommand, String[] args) {


        cliCommand.commandOf(Words.FILL_COMMAND)
                .setDescription("fill a schema with generated data");
        cliCommand.commandOf(LIST_COMMAND)
                .setDescription("list schemas");


        CliParser cliParser = Clis.getParser(cliCommand, args);

        List<CliCommand> commands = cliParser.getChildCommands();
        if (commands.size() > 0) {
            for (CliCommand command : commands) {
                switch (command.getName()) {
                    case FILL_COMMAND:
                        DbSchemaFill.run(command, args);
                        break;
                    case LIST_COMMAND:
                        DbSchemaList.run(command, args);
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
