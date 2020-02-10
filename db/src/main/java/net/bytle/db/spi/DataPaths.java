package net.bytle.db.spi;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a class with static utility to construct data path
 * If you want any other operations on a data path, go to the {@link Tabulars} class
 */
public class DataPaths {



  /**

   */
  public static DataPath sibling(DataPath dataPath, String name) {
    List<String> pathSegments = new ArrayList<>();
    pathSegments.addAll(dataPath.getNames().subList(0, dataPath.getNames().size() - 2));
    pathSegments.add(name);
    return dataPath.getDataStore().getDataPath(pathSegments.toArray(new String[0]));
  }








}
