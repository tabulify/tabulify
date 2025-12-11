package com.tabulify.engine;

import com.tabulify.dag.Dag;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;

import java.util.Collection;
import java.util.List;

/**
 * An utility class to create a dag of {@link DataPath}
 */
public class ForeignKeyDag {

  public static <T extends DataPath> Dag<T> createFromPaths(T dataPath) {
    if (Tabulars.isContainer(dataPath)) {
      //noinspection unchecked
      return (new Dag<T>().addRelations((List<? extends T>) Tabulars.getChildren(dataPath)));
    } else {
      return (new Dag<T>()).addRelation(dataPath);
    }
  }

  public static <T extends DataPath> Dag<T> createFromPaths(Collection<T> dataPaths) {

    return (new Dag<T>()).addRelations(dataPaths);

  }


}
