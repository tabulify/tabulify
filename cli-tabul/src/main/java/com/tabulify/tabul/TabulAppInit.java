package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.conf.ConfVault;
import com.tabulify.spi.DataPath;
import com.tabulify.cli.CliCommand;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TabulAppInit {
  public static List<DataPath> run(Tabular tabular, CliCommand subChildCommand) {

    Path resolve = Paths.get(".").resolve(Tabular.TABUL_CONF_FILE_NAME);
    ConfVault.createFromPath(resolve, tabular)
      .loadHowtoServices()
      .loadHowtoConnections()
      .flush();
    System.out.println("The configuration vault file has been created in the current directory. Location: " + resolve.toAbsolutePath());
    return new ArrayList<>();
  }

}
