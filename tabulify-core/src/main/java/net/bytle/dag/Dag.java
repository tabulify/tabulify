package net.bytle.dag;


import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;

/**
 * A class (direct acyclic graph) of dependencies
 * <p>
 * This class permits to:
 * * add dependency
 * * add also the dependency paths if {@link #setWithDependency(Boolean)} is set to true (ie foreign table for instance)
 * * get the dependencies in {@link #getCreateOrdered() create} or {@link #getDropOrdered()}
 */
public class Dag<T extends Dependency> {


  /**
   * The dependencies to start building the graph with
   */
  private final List<T> relationalList = new ArrayList<>();

  DefaultDirectedGraph<T, DefaultEdge> graph = null;

  /**
   * If set to true, the dag will add the foreign tables
   */
  private Boolean withDependency = false;

  public static <T extends Dependency> Dag<T> create() {
    return new Dag<>();
  }


  /**
   * Build the graph
   */
  void build() {

    // Building the graph
    graph = new DefaultDirectedGraph<>(DefaultEdge.class);

    for (T relation : relationalList) {
      addTableToVertex(relation);
    }

    CycleDetector<T, DefaultEdge> cycleDetector = new CycleDetector<>(graph);
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
        for (Dependency subRelation : subCycle) {
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
   * @param dependency recursive function
   */
  private void addTableToVertex(T dependency) {

    // Add the vertex
    if (!graph.containsVertex(dependency)) {
      graph.addVertex(dependency);
    }

    // Add the edges
    @SuppressWarnings("unchecked") Set<T> dependencies = (Set<T>) dependency.getDependencies();

    for (T parent : dependencies) {

      // Only if the table is in the list
      // or we take also the parent
      boolean relationContainsParent = false;
      for (T relation : relationalList) {
        if (parent.equals(relation)) {
          // We get the original object as it may have other specific attribute
          // Example: the backref group value as attribute for data path resource when selected via glob
          parent = relation;
          relationContainsParent = true;
          break;
        }
      }
      if (relationContainsParent || withDependency) {

        addTableToVertex(parent);

        // Add Edge
        graph.addEdge(parent, dependency);

      }


    }

  }


  /**
   * If set to true, the dag will add the parent table
   * (DataPathable defined in a foreign key)
   *
   * @return the dag for chaining initialization
   */
  public Dag<T> setWithDependency(Boolean aBoolean) {
    this.withDependency = aBoolean;
    return this;
  }

  public Dag<T> addRelation(T dependency) {
    if (!this.relationalList.contains(dependency)) {
      this.relationalList.add(dependency);
    }
    return this;
  }


  /**
   * Return an ordered list of tables ready to be loaded
   *
   */
  public List<T> getCreateOrdered() {

    if (graph == null) {
      build();
    }

    T currentDependency;
    T previousDependency = null;
    List<T> dependencies = new ArrayList<>();
    TopologicalOrderIterator<T, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(graph);

    // Because of the algorithm, we may have a table with a parent between table without parent
    // Little trick with on slot
    while (orderIterator.hasNext()) {
      currentDependency = orderIterator.next();

      final Set<? extends Dependency> parents = currentDependency.getDependencies();
      if (parents.isEmpty()) {
        dependencies.add(currentDependency);
      } else {
        if (previousDependency != null) {
          dependencies.add(previousDependency);
          dependencies.add(currentDependency);
          previousDependency = null;
        } else {
          previousDependency = currentDependency;
        }
      }
    }
    // Add the last one
    if (previousDependency != null) {
      dependencies.add(previousDependency);
    }
    return dependencies;

  }

  /**
   * Return an ordered list of tables ready to be dropped
   *
   */
  public List<T> getDropOrdered() {

    List<T> relations = getCreateOrdered();
    Collections.reverse(relations);
    return relations;

  }

  public Dag<T> addRelations(Collection<? extends T> relations) {
    relations.forEach(this::addRelation);
    return this;
  }

  public DefaultDirectedGraph<T, DefaultEdge> getGraph() {
    if (graph == null) {
      build();
    }
    return graph;
  }
}
