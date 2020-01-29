package net.bytle.db.engine;



import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;


public class Dag {


    /**
     * The tables to start building the graph with
     */
    private List<DataPath> dataPathList = new ArrayList<>();

    DefaultDirectedGraph<DataPath, DefaultEdge> g = null;

    /**
     * If set to true, the dag will add the foreign tables
     */
    private Boolean withForeignTable = false;

    /**
     * Use the {@link #get(List)}
     */
    private Dag() {
    }

    public static Dag get(List<DataPath> dataPaths) {

        Dag dag = Dag.get();
        for (DataPath dataPath : dataPaths) {
            dag.addTable(dataPath);
        }
        return dag;

    }

    /**
     * Build the graph
     */
    void build() {

        // Building the graph
        g = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (DataPath dataPath : dataPathList) {
            addTableToVertex(dataPath);
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
                for (DataPath subDataPath : subCycle) {
                    // The Remove vertex that this cycle is not encountered again is not necessary as we throw an exception
                    // but I let it for information
                    // cycleVertices.remove(sub);
                    throw new RuntimeException("A cycle was detected with the following data  " + subDataPath.toString());
                }
            }
        }
    }

    /**
     * Add one table to the vertex
     *
     * @param dataPath recursive function
     */
    private void addTableToVertex(DataPath dataPath) {

        // Add the vertex
        if (!g.containsVertex(dataPath)) {
            g.addVertex(dataPath);
        }

        // Add the edges
        List<ForeignKeyDef> foreignKeys = dataPath.getDataDef() != null ? dataPath.getDataDef().getForeignKeys() : new ArrayList<>();
        if (foreignKeys.size() > 0) {

            for (ForeignKeyDef foreignKeyDef : foreignKeys) {

                DataPath foreignDataPath = foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath();

                // Only if the table is in the list
                // or we take also the parent
                if (dataPathList.contains(foreignDataPath) || withForeignTable) {

                    // Because we don't have any cache the object in the dataPathList
                    // may not be the same than foreignDataPath (ie the properties for instance may differ)
                    // Hack to get the object from the list
                    if (dataPathList.contains(foreignDataPath)){
                        for (DataPath dataPathInList: dataPathList){
                            if (dataPathInList.equals(foreignDataPath)){
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

    }



    private static Dag get() {
        return new Dag();
    }

    public static Dag get(DataPath dataPath) {
        if (Tabulars.isContainer(dataPath)) {
            return get(Tabulars.getChildren(dataPath));
        } else {
            return new Dag().addTable(dataPath);
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

    private Dag addTable(DataPath dataPath) {
        if (!this.dataPathList.contains(dataPath)) {
            this.dataPathList.add(dataPath);
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

            final List<ForeignKeyDef> foreignKeys = currentDataPath.getDataDef() !=null ? currentDataPath.getDataDef().getForeignKeys() : new ArrayList<>();
            if (foreignKeys.size() == 0) {
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

        List<DataPath> dataPaths = getCreateOrderedTables();
        Collections.reverse(dataPaths);
        return dataPaths;

    }

}
