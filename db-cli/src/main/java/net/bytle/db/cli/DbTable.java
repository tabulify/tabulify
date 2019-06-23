package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.DbLoggers;

import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.cli.Words.*;


/**
 * Created by gerard on 08-12-2016.
 * <p>
 */
public class DbTable {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_CLI;


    public static void run(CliCommand cliCommand, String[] args) {


        cliCommand.commandOf(Words.LOAD_COMMAND)
                .setDescription("load data into a table from a file");
        cliCommand.commandOf(Words.DOWNLOAD_COMMAND)
                .setDescription("download a table into a file");
        cliCommand.commandOf(Words.TRANSFER_COMMAND)
                .setDescription("transfer a table from a database to another");
        cliCommand.commandOf(Words.FILL_COMMAND)
                .setDescription("fill a table with generated data");
        cliCommand.commandOf(Words.LIST_COMMAND)
                .setDescription("list tables");
        cliCommand.commandOf(Words.DROP_COMMAND)
                .setDescription("drop table(s)");
        cliCommand.commandOf(Words.COUNT_COMMAND)
                .setDescription("count the number of tables");
        cliCommand.commandOf(Words.DESCRIBE_COMMAND)
                .setDescription("show the table structures");
        cliCommand.commandOf(Words.SHOW_COMMAND)
                .setDescription("show the data of the table");

        CliParser cliParser = Clis.getParser(cliCommand, args);

        List<CliCommand> commands = cliParser.getChildCommands();
        if (commands.size() > 0) {
            for (CliCommand command : commands) {
                switch (command.getName()) {
                    case LOAD_COMMAND:
                        DbTableLoad.run(command, args);
                        break;
                    case DOWNLOAD_COMMAND:
                        DbTableDownload.run(command, args);
                        break;
                    case TRANSFER_COMMAND:
                        DbTableTransfer.run(command, args);
                        break;
                    case FILL_COMMAND:
                        DbTableFill.run(command, args);
                        break;
                    case DESCRIBE_COMMAND:
                        DbTableDescribe.run(command, args);
                        break;
                    case SHOW_COMMAND:
                        DbTableShow.run(command, args);
                        break;
                    case LIST_COMMAND:
                        DbTableList.run(command, args);
                        break;
                    case DROP_COMMAND:
                        DbTableDrop.run(command, args);
                        break;
                    case COUNT_COMMAND:
                        DbTableCount.run(command, args);
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
