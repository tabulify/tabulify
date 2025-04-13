package com.tabulify.tabli;

import com.tabulify.Tabular;
import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.spi.DataPath;
import net.bytle.cli.CliCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that prints diagnostic information
 */
public class TabliDiagnostic {

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    System.out.println();
    System.out.println("List of installed Tabulify file managers:");
    List<FsFileManagerProvider> fsFileManagerProviders = FsFileManagerProvider.installedProviders();
    for (FsFileManagerProvider filemanagerProvider : fsFileManagerProviders) {
      System.out.println(filemanagerProvider.getClass());
    }
    System.out.println();

    return new ArrayList<>();

  }
}
