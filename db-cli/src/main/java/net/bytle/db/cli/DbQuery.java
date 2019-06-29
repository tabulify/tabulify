package net.bytle.db.cli;


import net.bytle.cli.*;
import net.bytle.db.DbLoggers;

import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.*;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 */
public class DbQuery {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;


    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.commandOf(Words.EXECUTE_COMMAND)
                .setDescription("execute a query");
        cliCommand.commandOf(Words.DOWNLOAD_COMMAND)
                .setDescription("download a query into a file");
        cliCommand.commandOf(Words.TRANSFER_COMMAND)
                .setDescription("transfer the query from one database to another");
        cliCommand.commandOf(Words.DIFF_COMMAND)
                .setDescription("diff of two query results");

        CliParser cliParser = Clis.getParser(cliCommand, args);

        List<CliCommand> commands = cliParser.getChildCommands();
        if (commands.size() == 0) {

            LOGGER.severe("A known command must be given.");
            CliUsage.print(cliCommand);
            System.exit(1);

        }

        for (CliCommand command : commands) {
            switch (command.getName()) {
                case EXECUTE_COMMAND:
                    DbQueryExecute.run(command, args);
                    break;
                case DOWNLOAD_COMMAND:
                    DbQueryDownload.run(command, args);
                    break;
                case TRANSFER_COMMAND:
                    DbQueryTransfer.run(command, args);
                    break;
                case DIFF_COMMAND:
                    DbQueryDiff.run(command, args);
                    break;
                default:
                    LOGGER.severe("The command (" + command + ") is unknown");
                    CliUsage.print(cliCommand);
                    System.exit(1);
            }

        }

    }

}
