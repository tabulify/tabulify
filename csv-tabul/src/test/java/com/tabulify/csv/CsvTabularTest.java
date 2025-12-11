package com.tabulify.csv;

import com.tabulify.Tabular;
import com.tabulify.fs.FsDataPath;
import com.tabulify.spi.Tabulars;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class CsvTabularTest {


  @Test
  public void print() throws URISyntaxException {
    try (Tabular tabular = Tabular.tabularWithoutConfigurationFile()) {
      URL resource = Objects.requireNonNull(CsvTabularTest.class.getResource("/db-fs/csv/test.csv"));
      Path path = Paths.get(resource.toURI());
      FsDataPath dataPath = tabular.getDataPath(path);
      Assertions.assertInstanceOf(CsvDataPath.class, dataPath);
      Tabulars.print(dataPath);
    }
  }
}
