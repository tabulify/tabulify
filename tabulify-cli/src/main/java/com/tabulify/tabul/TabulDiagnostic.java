package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.spi.ConnectionProvider;
import com.tabulify.spi.DataPath;
import net.bytle.cli.CliCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that prints diagnostic information
 */
public class TabulDiagnostic {

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    System.out.println();

    List<FsFileManagerProvider> fsFileManagerProviders = FsFileManagerProvider.installedProviders();
    if (fsFileManagerProviders.isEmpty()) {
      System.out.println("No Tabulify file managers could be found");
    } else {
      System.out.println("List of installed Tabulify file managers:");
      for (FsFileManagerProvider filemanagerProvider : fsFileManagerProviders) {
        System.out.println(filemanagerProvider.getClass());
      }
    }
    System.out.println();

    List<ConnectionProvider> installedProviders = ConnectionProvider.installedProviders();
    if (installedProviders.isEmpty()) {
      System.out.println("No Tabulify connection managers could be found");
    } else {
      System.out.println("List of installed Tabulify Connection managers:");
      for (ConnectionProvider connectionProvider : installedProviders) {
        System.out.println(connectionProvider.getClass());
      }
    }
    System.out.println();

    return new ArrayList<>();

  }
}
