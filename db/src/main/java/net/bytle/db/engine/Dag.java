package net.bytle.db.engine;


import net.bytle.db.spi.DataPath;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;


public class Dag<T extends Relational> {


  /**
   * The tables to start building the graph with
   */
  private List<T> relationalList = new ArrayList<>();

  DefaultDirectedGraph<T, DefaultEdge> g = null;

  /**
   * If set to true, the dag will add the foreign tables
   */
  private Boolean withForeignTable = false;

  /**
   * You may also use the {@link DagDataPath#get(DataPath)} function
   */
  Dag() {
  }


  /**
   * Build the graph
   */
  void build() {

    // Building the graph
    g = new DefaultDirectedGraph<>(DefaultEdge.class);

    for (T relation : relationalList) {
      addTableToVertex(relation);
    }

    CycleDetector<T, DefaultEdge> cycleDetector = new CycleDetector<>(g);
    if (cycleDetector.detectCycles()) {

      System.out.println("Cycles detected.");

      // Get all vertices involved in cycles.
      Set<T> cycleVertices = cycleDetector.findCycles();

      // Loop through vertices trying to find disjoint cycles.
      while (!cycleVertices.isEmpty()) {
        System.out.println("Cycle:");

        // Get a vertex involved in a cycle.
        Iterator<T> iterator = cycleVertices.iterator();
        T cycle = iterator.next();

        // Get all vertices involved with this vertex.
        Set<T> subCycle = cycleDetector.findCyclesContainingVertex(cycle);
        for (T subRelation : subCycle) {
          // The Remove vertex that this cycle is not encountered again is not necessary as we throw an exception
          // but I let it for information
          // cycleVertices.remove(sub);
          throw new RuntimeException("A cycle was detected with the following data  " + subRelation.toString());
        }
      }
    }
  }

  /**
   * Add one table to the vertex
   *
   * @param dataPath recursive function
   */
  private void addTableToVertex(T dataPath) {

    // Add the vertex
    if (!g.containsVertex(dataPath)) {
      g.addVertex(dataPath);
    }

    // Add the edges
    List<T> parents = dataPath.getParents();
    for (T foreignDataPath : parents) {
      // Only if the table is in the list
      // or we take also the parent
      if (relationalList.contains(foreignDataPath) || withForeignTable) {

        // Because we don't have any cache the object in the dataPathList
        // may not be the same than foreignDataPath (ie the properties for instance may differ)
        // Hack to get the object from the list
        if (relationalList.contains(foreignDataPath)) {
          for (T dataPathInList : relationalList) {
            if (dataPathInList.equals(foreignDataPath)) {
              foreignDataPath = dataPathInList;
              break;
            }
          }
        }

        addTableToVertex(foreignDataPath);

        // Add Edge
        g.addEdge(foreignDataPath, dataPath);

      }


    }

  }



  /**
   * If set to true, the dag will add the parent table
   * (Table defined in a foreign key)
   *
   * @param aBoolean
   * @return the dag for chaining initialization
   */
  public Dag setWithForeignTable(Boolean aBoolean) {
    this.withForeignTable = aBoolean;
    return this;
  }

  Dag<T> addRelation(T dataPath) {
    if (!this.relationalList.contains(dataPath)) {
      this.relationalList.add(dataPath);
    }
    return this;
  }


  /**
   * Return an ordered list of tables ready to be loaded
   *
   * @return
   */
  public List<T> getCreateOrderedTables() {

    if (g == null) {
      build();
    }

    T currentDataPath = null;
    T previousDataPath = null;
    List<T> dataPaths = new ArrayList<>();
    TopologicalOrderIterator<T, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(g);

    // Because of the algorithm, we may have a table with a parent between table without parent
    // Little trick with on slot
    while (orderIterator.hasNext()) {
      currentDataPath = orderIterator.next();

      final List<T> parents = currentDataPath.getParents();
      if (parents.size() == 0) {
        dataPaths.add(currentDataPath);
      } else {
        if (previousDataPath != null) {
          dataPaths.add(previousDataPath);
          dataPaths.add(currentDataPath);
          previousDataPath = null;
        } else {
          previousDataPath = currentDataPath;
        }
      }
    }
    // Add the last one
    if (previousDataPath != null) {
      dataPaths.add(currentDataPath);
    }
    return dataPaths;

  }

  /**
   * Return an ordered list of tables ready to be dropped
   *
   * @return
   */
  public List<T> getDropOrderedTables() {

    List<T> relations = getCreateOrderedTables();
    Collections.reverse(relations);
    return relations;

  }

  public Dag<T> addRelations(List<T> relations) {
    relations.forEach(this::addRelation);
    return this;
  }


}
