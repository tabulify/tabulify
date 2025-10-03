package com.tabulify.tpc;


import com.tabulify.connection.Connection;
import net.bytle.exec.Command;
import net.bytle.fs.Fs;
import net.bytle.os.Oss;

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


  private final Connection connection;
  private Path outputDirectory;

  private Path queryTemplatesDirectory = Paths.get(".");
  private Path distributionFile = Paths.get(".");
  private String dialect;
  private Path dsqgenDirectory;

  public TpcdsQgen(Connection connection) {
    this.connection = connection;
  }

  public static TpcdsQgen create(Connection connection) {
    return new TpcdsQgen(connection);
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

    /**
     * Args of dsqgen building
     */
    List<String> args = new ArrayList<>();
    String startParameter = "-";
    if (Oss.isWindows()) {
      startParameter = "/";
    }
    args.add(startParameter + "directory");
    args.add(queryTemplatesDirectory.toAbsolutePath().normalize().toString());
    if (dialect != null) {
      args.add(startParameter + "dialect");
      args.add(dialect);
    }
    args.add(startParameter + "distributions");
    args.add(String.valueOf(distributionFile.toAbsolutePath().normalize()));

    /**
     * Output target directory
     */
    Path targetDirectory = Paths.get("./src/main/sql/tpcds/query/" + connection.getName());
    if (outputDirectory != null) {
      targetDirectory = outputDirectory;
    }
    Fs.createDirectoryIfNotExists(targetDirectory);

    /**
     * What is that ?
     */
    Path queryOutput = Paths.get(targetDirectory.toString(), "query_0.sql");
    int queryTemplatesCount = 99;

    /**
     * dsqgenExePath path
     */
    String dsqgenExe = "dsqgen";
    Path dsqgenExePath = Paths.get(dsqgenExe);
    if (dsqgenDirectory != null) {
      dsqgenExePath = dsqgenDirectory.resolve(dsqgenExe);
    }

    for (int i = 1; i <= queryTemplatesCount; i++) {


      Command command = Command.create(dsqgenExePath.toAbsolutePath().toString())
        .addArgs(args)
        .addArg(startParameter + "template")
        .addArg("query" + i + ".tpl")
        .setWorkingDirectory(targetDirectory);
      command.startAndWait();
      if (command.getExitValue() != 0) {
        System.err.println("Error while generating the sql");
        System.err.println(command.getOutput());
        System.exit(1);
      } else {
        System.out.println("Query " + i + " generated to " + queryOutput);
      }
      Path queryOutputDest = Paths.get(targetDirectory.toString(), "query_" + i + ".sql");
      try {
        Files.move(queryOutput, queryOutputDest, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Query " + i + " moved to " + queryOutputDest);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return queryTemplatesCount + " created";
  }

  /**
   * The dialect value:
   * * `sqlite`
   * * ...
   *
   * @param dialect <a href="https://datacadamia.com/data/type/relation/benchmark/tpcds/dsqgen#dialect">Dialect</a>
   */
  public TpcdsQgen setDialect(String dialect) {
    this.dialect = dialect;
    return this;
  }

  public TpcdsQgen setDsqGenDirectory(Path directory) {
    this.dsqgenDirectory = directory;
    return this;
  }
}
