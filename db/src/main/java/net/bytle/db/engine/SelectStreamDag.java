package net.bytle.db.engine;

import net.bytle.db.spi.DataPath;

import java.util.List;

public class SelectStreamDag {



  public static Dag get(List<DataPath> dataPaths) {

    return (new Dag(Dag.SELECT_STREAM_DEPENDENCY)).addRelations(dataPaths);

  }

  public static Dag get(DataPath dataPath) {

    return (new Dag(Dag.SELECT_STREAM_DEPENDENCY)).addRelation(dataPath);

  }


}
