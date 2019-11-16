package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.log.Log;

import java.util.*;

import static net.bytle.db.cli.Words.*;

/**
 * Created by gerard on 31-05-2017.
 */
public class DbXml {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;
    public static final String CSV = "csv";

    // To store the columns header
    public static Set<String> headers = new HashSet<>();

    // To store  the data
    public static List<Map<String, String>> records = new ArrayList<>();
    private static CliParser cliParser;

    public static void run(CliCommand cliCommand, String[] args) {


        // A client command example
        String example = "To of the text value of the node defined by an Xpath expression, you would type:\n" +
                CliUsage.getFullChainOfCommand(cliCommand) + " of --xpath \"//Repository/DECLARE/Database[@name=\\\"databaseName\\\"]/\" -in inputFile.xml \n";


        cliCommand.commandOf(Words.EXTRACT)
                .setDescription("extract one or multiple nodes and create another xml");
        cliCommand.commandOf(Words.PRINT)
                .setDescription("print the XML tree as seen by the DOM");
        cliCommand.commandOf(Words.CHECK)
                .setDescription("verify the value of a node or from an attribute");
        cliCommand.commandOf(Words.UPDATE)
                .setDescription("update the value of a node or from an attribute");
        cliCommand.commandOf(Words.GET)
                .setDescription("return the value of a node or from an attribute");
        cliCommand.commandOf(Words.STRUCTURE)
                .setDescription("print a tree summary structure of an xml");
        cliCommand.commandOf(CSV)
            .setDescription("create form an XML file a CSV file");

        // Initiate the client helper
        cliCommand.setDescription("Xml utility")
                .addExample(example);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        List<CliCommand> commands = cliParser.getChildCommands();
        if (commands.size() > 0) {
            for (CliCommand command : commands) {
                switch (command.getName()) {
                    case EXTRACT:
                        DbXmlExtract.run(command, args);
                        break;
                    case PRINT:
                        DbXmlPrint.run(command, args);
                        break;
                    case CHECK:
                        DbXmlCheck.run(command, args);
                        break;
                    case UPDATE:
                        DbXmlUpdate.run(command, args);
                        break;
                    case GET:
                        DbXmlGet.run(command, args);
                        break;
                    case STRUCTURE:
                        DbXmlStructure.run(command, args);
                        break;
                    case CSV:
                        DbXml2Csv.run(command, args);
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
