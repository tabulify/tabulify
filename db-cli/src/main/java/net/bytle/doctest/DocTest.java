package net.bytle.doctest;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.Clis;
import net.bytle.db.cli.Db;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Fs;
import net.bytle.db.engine.SchemaManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static net.bytle.db.cli.Db.JDBC_URL_TARGET_DEFAULT;

public class DocTest {

    final static String CLI_NAME = "doctest";
    final static String PATH = "path";

    final static Logger LOGGER = DocTestLogger.LOGGER_DOCTEST;

    public static void main(String[] args) {


        CliCommand cli = Clis.getCli(CLI_NAME)
                .setDescription("A runner of code in documentation creating the output");

        cli.argOf(PATH)
                .setDescription("The path to doc file or to a directory")
                .setMandatory(true);


        CliParser cliParser = Clis.getParser(cli, args);

        final Path path = cliParser.getPath(PATH);
        if (!Files.exists(path)) {
            LOGGER.severe("The path (" + path + ") does not exist");
            System.exit(1);
        }
        List<Path> paths = Fs.getChildFiles(path);

        int errorCount = 0;
        DocTestRunner docTestRunner = DocTestRunner.get();

        for (Path childPath : paths) {

            // TODO: The database name is set to the test database to avoid sqlite blocking
            Database database = Databases.get(Db.CLI_DATABASE_NAME_TARGET);
            database.setUrl(JDBC_URL_TARGET_DEFAULT);
            SchemaManager.dropAllTables(database.getCurrentSchema());


            DocTestRunResult docTestRunResult = docTestRunner.run(childPath);
            errorCount += docTestRunResult.getErrors();
            Fs.toFile(docTestRunResult.getNewDoc(), childPath);

        }

        if (errorCount != 0) {
            final String msg = errorCount + " errors were seen during documentation execution.";
            System.err.println(msg);
            LOGGER.severe(msg);
            System.exit(errorCount);
        }

    }


}

