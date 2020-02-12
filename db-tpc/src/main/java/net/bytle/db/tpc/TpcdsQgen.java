package net.bytle.db.tpc;

import net.bytle.command.Command;
import net.bytle.db.database.DataStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * To generate the query
 */
public class TpcdsQgen {


    private final DataStore dataStore;
    private Path outputDirectory;

    private String dsqgenExe = "dsqgen.exe";
    private Path queryTemplatesDirectory = Paths.get(".");
    private Path distributionFile = Paths.get(".");

    public TpcdsQgen(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public static TpcdsQgen get(DataStore dataStore) {
        return new TpcdsQgen(dataStore);
    }

    public TpcdsQgen setOutputDirectory(Path path) {
        this.outputDirectory = path;
        return this;
    }

    public TpcdsQgen setQueryTemplatesDirectory(Path path) {
        this.queryTemplatesDirectory = path;
        return this;
    }

    public TpcdsQgen setDistributionFile(Path path) {
        this.distributionFile = path;
        return this;
    }

    public String start() {
        List<String> args = new ArrayList<>();
        args.add("/directory");
        args.add(queryTemplatesDirectory.toAbsolutePath().normalize().toString());
        args.add("/dialect sqlite");
        args.add("/distributions " + distributionFile.toAbsolutePath().normalize());

        Path workingDirectory = Paths.get("./src/main/sql/tpcds/query/sqlite");
        Path queryOutput = Paths.get(workingDirectory.toString(), "query_0.sql");
        int queryTemplatesCount = 99;
        for (int i = 1; i <= queryTemplatesCount; i++) {

            Command command = Command.get(dsqgenExe)
                    .addArgs(args)
                    .addArg("/template query" + i + ".tpl")
                    .setWorkingDirectory(workingDirectory);
            command.startAndWait();
            if (command.getExitValue() != 0) {
                System.err.println("Error while generating the sql");
                System.err.println(command.getOutput());
                System.exit(1);
            } else {
                System.out.println("Query " + i + " generated to " + queryOutput);
            }
            Path queryOutputDest = Paths.get(workingDirectory.toString(), "query_" + i + ".sql");
            try {
                Files.move(queryOutput, queryOutputDest, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Query " + i + " moved to " + queryOutputDest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return queryTemplatesCount + " created";
    }
}
