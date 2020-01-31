package net.bytle.db.engine;

import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;

import java.util.List;

public class ForeignKeyDag {

  public static Dag get(DataPath dataPath) {
    if (Tabulars.isContainer(dataPath)) {
      return (new Dag(Dag.FOREIGN_KEY_RELATIONSHIP)).addRelations(Tabulars.getChildren(dataPath));
    } else {
      return (new Dag(Dag.FOREIGN_KEY_RELATIONSHIP)).addRelation(dataPath);
    }
  }

  public static Dag get(List<DataPath> dataPaths) {

    return (new Dag(Dag.FOREIGN_KEY_RELATIONSHIP)).addRelations(dataPaths);

  }


}
