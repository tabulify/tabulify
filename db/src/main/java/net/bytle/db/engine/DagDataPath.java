package net.bytle.db.engine;

import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;

import java.util.List;

public class DagDataPath {

  public static Dag<DataPath> get(DataPath dataPath) {
    if (Tabulars.isContainer(dataPath)) {
      return (new Dag<DataPath>()).addRelations(Tabulars.getChildren(dataPath));
    } else {
      return (new Dag<DataPath>()).addRelation(dataPath);
    }
  }

  public static Dag<DataPath> get(List<DataPath> dataPaths) {

    return (new Dag<DataPath>()).addRelations(dataPaths);

  }


}
