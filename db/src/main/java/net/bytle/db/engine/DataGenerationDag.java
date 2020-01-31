package net.bytle.db.engine;

import net.bytle.db.spi.DataPath;

import java.util.List;

public class DataGenerationDag {



  public static Dag get(List<DataPath> dataPaths) {

    return (new Dag(Dag.DATA_GENERATION_RELATIONSHIP)).addRelations(dataPaths);

  }


}
