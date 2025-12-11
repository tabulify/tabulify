package com.tabulify.zip.datapath;

import com.tabulify.Tabular;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ArchiveDataPathTest {

  @Test
  void count() {

    try (Tabular tabular = Tabular.tabularWithoutConfigurationFile()) {

      DataPath dataPath = tabular.getResourceDataPath(ArchiveDataPathTest.class, "archives/archive.zip");
      Assertions.assertEquals(ArchiveDataPath.class, dataPath.getClass());
      RelationDef relationDef = dataPath.getOrCreateRelationDef();
      Assertions.assertEquals(4, relationDef.getColumnDefs().size());
      Assertions.assertEquals(3, dataPath.getCount());
      Tabulars.print(dataPath);

    }

  }
}
