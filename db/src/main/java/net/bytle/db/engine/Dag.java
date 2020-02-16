package net.bytle.db.engine;


import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.spi.DataPath;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;


public class Dag {

  /**
   * A foreign key relationship is defined by a foreign key
   */
  protected static final String FOREIGN_KEY_DEPENDENCY = "Foreign Key";


  /**
   * A variable to hold the relationship value
   */
  private final String dependencyType;

  /**
   * The tables to start building the graph with
   */
  private List<DataPath> relationalList = new ArrayList<>();

  DefaultDirectedGraph<DataPath, DefaultEdge> g = null;

  /**
   * If set to true, the dag will add the foreign tables
   */
  private Boolean withDependency = false;

  /**
   * You may also use the {@link ForeignKeyDag#get(DataPathAbs)} function
   */
  Dag(String dependencyType) {
    this.dependencyType = dependencyType;
  }


  /**
   * Build the graph
   */
  void build() {

    // Building the graph
    g = new DefaultDirectedGraph<>(DefaultEdge.class);

    for (DataPath relation : relationalList) {
      addTableToVertex(relation);
    }

    CycleDetector<DataPath, DefaultEdge> cycleDetector = new CycleDetector<>(g);
    if (cycleDetector.detectCycles()) {

      System.out.println("Cycles detected.");

      // Get all vertices involved in cycles.
      Set<DataPath> cycleVertices = cycleDetector.findCycles();

      // Loop through vertices trying to find disjoint cycles.
      while (!cycleVertices.isEmpty()) {
        System.out.println("Cycle:");

        // Get a vertex involved in a cycle.
        Iterator<DataPath> iterator = cycleVertices.iterator();
        DataPath cycle = iterator.next();

        // Get all vertices involved with this vertex.
        Set<DataPath> subCycle = cycleDetector.findCyclesContainingVertex(cycle);
        for (DataPath subRelation : subCycle) {
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
   * @param relation recursive function
   */
  private void addTableToVertex(DataPath relation) {

    // Add the vertex
    if (!g.containsVertex(relation)) {
      g.addVertex(relation);
    }

    // Add the edges
    List<DataPath> dependencies;
    switch (this.dependencyType){
      case FOREIGN_KEY_DEPENDENCY:
        dependencies = relation.getForeignKeyDependencies();
        break;
      default:
        throw new RuntimeException("This dependency type ("+this.dependencyType +") is unknown");
    }

    for (DataPath parent : dependencies) {
      // Only if the table is in the list
      // or we take also the parent
      if (relationalList.contains(parent) || withDependency) {

        // Because we don't have any cache the object in the dataPathList
        // may not be the same than foreignDataPath (ie the properties for instance may differ)
        // Hack to get the object from the list
        if (relationalList.contains(parent)) {
          for (DataPath relationInList : relationalList) {
            if (relationInList.equals(parent)) {
              parent = relationInList;
              break;
            }
          }
        }

        addTableToVertex(parent);

        // Add Edge
        g.addEdge(parent, relation);

      }


    }

  }


  /**
   * If set to true, the dag will add the parent table
   * (DataPathable defined in a foreign key)
   *
   * @param aBoolean
   * @return the dag for chaining initialization
   */
  public Dag setWithDependency(Boolean aBoolean) {
    this.withDependency = aBoolean;
    return this;
  }

  Dag addRelation(DataPath dataPath) {
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
  public List<DataPath> getCreateOrderedTables() {

    if (g == null) {
      build();
    }

    DataPath currentDataPath = null;
    DataPath previousDataPath = null;
    List<DataPath> dataPaths = new ArrayList<>();
    TopologicalOrderIterator<DataPath, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(g);

    // Because of the algorithm, we may have a table with a parent between table without parent
    // Little trick with on slot
    while (orderIterator.hasNext()) {
      currentDataPath = orderIterator.next();

      final List<DataPath> parents = currentDataPath.getForeignKeyDependencies();
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
  public List<DataPath> getDropOrderedTables() {

    List<DataPath> relations = getCreateOrderedTables();
    Collections.reverse(relations);
    return relations;

  }

  public Dag addRelations(List<DataPath> relations) {
    relations.forEach(this::addRelation);
    return this;
  }


}
