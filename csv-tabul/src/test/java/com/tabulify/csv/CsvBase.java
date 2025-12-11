package com.tabulify.csv;

import com.tabulify.Tabular;
import com.tabulify.fs.FsConnection;
import com.tabulify.type.KeyNormalizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class CsvBase {

  Tabular tabular;
  FsConnection resourceDataStore;
  public static final String CSV_RESOURCE_ROOT = "db-fs/csv";

  @BeforeEach
  public void setUp() throws Exception {
    tabular = Tabular.tabularWithoutConfigurationFile();
    resourceDataStore = tabular.createRuntimeConnectionForResources(CsvBase.class, CSV_RESOURCE_ROOT, KeyNormalizer.createSafe("resources"));
  }

  @AfterEach
  void tearDown() {

    tabular.close();
    tabular = null;
  }

}
