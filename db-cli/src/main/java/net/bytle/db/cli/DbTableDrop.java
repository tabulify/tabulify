package net.bytle.db.cli;


import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.cli.Clis;
import net.bytle.db.DatastoreVault;
import net.bytle.db.engine.ForeignKeyDag;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.uri.DataUri;
import net.bytle.log.Log;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static java.lang.System.exit;
import static net.bytle.db.cli.Words.*;


public class DbTableDrop {

    private static final Log LOGGER = Db.LOGGER_DB_CLI;

    private static final String TABLE_URIS = "tableUri...";

    public static void run(CliCommand cliCommand, String[] args) {

        String description = "Drop table(s).";
        String example = "";
        example += "To drop the tables D_TIME and F_SALES:\n\n" +
                CliUsage.TAB + CliUsage.getFullChainOfCommand(cliCommand) + "D_TIME@datastore F_SALES@datastore\n\n";
        example += "To drop only the table D_TIME with force (ie deleting the foreign keys constraint):\n\n" +
                CliUsage.TAB + CliUsage.getFullChainOfCommand(cliCommand) + CliParser.PREFIX_LONG_OPTION + FORCE + "@database/D_TIME\n\n";
        example += "To drop all dimension tables that begins with (D_):\n\n" +
                CliUsage.TAB + CliUsage.getFullChainOfCommand(cliCommand) + " D_*@datastore\n\n";
        example += "To drop all tables:\n\n" +
                CliUsage.TAB + CliUsage.getFullChainOfCommand(cliCommand) + " *@database\n\n";

        // Create the parser
        cliCommand
                .setDescription(description)
                .addExample(example);


        cliCommand.argOf(TABLE_URIS)
                .setDescription("One or more table URI")
                .setMandatory(true);

        cliCommand.flagOf(FORCE)
                .setDescription("if set, the foreign keys referencing the tables to drop will be dropped");

        cliCommand.flagOf(NO_STRICT)
                .setDescription("if set, it will not throw an error if a table is not found")
                .setDefaultValue(false);

        cliCommand.optionOf(DATASTORE_VAULT_PATH);

        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATASTORE_VAULT_PATH);
        DatastoreVault datastoreVault = DatastoreVault.of(storagePathValue);


        // Bring the of statement out of the output zone
        // Otherwise we will not see them their log in the output stream
        final Boolean withForce = cliParser.getBoolean(FORCE);
        final Boolean notStrict = cliParser.getBoolean(NO_STRICT);


        // Get the tables asked
        List<String> tableUris = cliParser.getStrings(TABLE_URIS);
        List<DataPath> selectedDataPaths = new ArrayList<>();
        for (String dataUri : tableUris) {
            List<DataPath> select = DataPaths.select(datastoreVault, DataUri.of(dataUri));
            if (select.size() == 0) {
                final String msg = "No tables found with the data Uri (" + dataUri + ")";
                if (notStrict) {
                    LOGGER.warning(msg);
                } else {
                    LOGGER.severe(msg);
                    exit(1);
                }
            }
            selectedDataPaths.addAll(select);
        }


        // Doing the work
        System.out.println();

        for (DataPath dataPathToDrop : ForeignKeyDag.get(selectedDataPaths).getDropOrderedTables()) {

            List<DataPath> referenceDataPaths = Tabulars.getReferences(dataPathToDrop);
            for (DataPath referenceDataPath : referenceDataPaths) {
                if (!selectedDataPaths.contains(referenceDataPath)) {
                    if (withForce) {

                        List<ForeignKeyDef> droppedForeignKeys = Tabulars.dropOneToManyRelationship(referenceDataPath,dataPathToDrop);
                        droppedForeignKeys.stream()
                                .forEach(fk->LOGGER.warning("ForeignKey (" + fk.getName() + ") was dropped from the table (" + fk.getTableDef().getDataPath() + ")"));

                    } else {
                        LOGGER.severe("The table (" + referenceDataPath + ") is referencing the table (" + dataPathToDrop + ") and is not in the tables to drop");
                        LOGGER.severe("To drop the foreign keys referencing the tables to drop, you can add the force flag (" + CliParser.PREFIX_LONG_OPTION + Words.FORCE + ").");
                        LOGGER.severe("Exiting");
                        System.exit(1);
                    }
                }
            }

            Tabulars.drop(dataPathToDrop);
            LOGGER.info("Table (" + dataPathToDrop + ") was dropped.");
        }

        // End

        // Setting the log back to see them in a test
        LOGGER.setLevel(Level.INFO);
        LOGGER.info("Bye !");

    }


}
