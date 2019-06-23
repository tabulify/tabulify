package net.bytle.db.engine;


import com.google.common.collect.Lists;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.SchemaDef;
import net.bytle.db.model.TableDef;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class Dag {

    /**
     * The tables to start building the graph with
     */
    private List<TableDef> tableDefList = new ArrayList<>();

    DefaultDirectedGraph<TableDef, DefaultEdge> g = null;

    /**
     * If set to true, the dag will add the foreign tables
     */
    private Boolean withForeignTable = false;

    /**
     * Use the {@link #get(List)} or {@link #get(SchemaDef)}
     */
    private Dag() {
    }

    public static Dag get(List<TableDef> tables) {

        Dag dag = Dag.get();
        for (TableDef tableDef : tables) {
            dag.addTable(tableDef);
        }
        return dag;

    }

    /**
     * Build the graph
     */
    void build() {

        // Building the graph
        g = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (TableDef tableDef : tableDefList) {
            addTableToVertex(tableDef);
        }

        CycleDetector<TableDef, DefaultEdge> cycleDetector = new CycleDetector<>(g);
        if (cycleDetector.detectCycles()) {

            System.out.println("Cycles detected.");

            // Get all vertices involved in cycles.
            Set<TableDef> cycleVertices = cycleDetector.findCycles();

            // Loop through vertices trying to find disjoint cycles.
            while (!cycleVertices.isEmpty()) {
                System.out.println("Cycle:");

                // Get a vertex involved in a cycle.
                Iterator<TableDef> iterator = cycleVertices.iterator();
                TableDef cycle = iterator.next();

                // Get all vertices involved with this vertex.
                Set<TableDef> subCycle = cycleDetector.findCyclesContainingVertex(cycle);
                for (TableDef sub : subCycle) {
                    // The Remove vertex that this cycle is not encountered again is not necessary as we throw an exception
                    // but I let it for information
                    // cycleVertices.remove(sub);
                    throw new RuntimeException("A cycle was deteced with the following table " + sub.getFullyQualifiedName());
                }
            }
        }
    }

    /**
     * Add one table to the vertex
     *
     * @param tableDef recursive function
     */
    private void addTableToVertex(TableDef tableDef) {

        // Add the vertex
        if (!g.containsVertex(tableDef)) {
            g.addVertex(tableDef);
        }

        // Add the edges
        List<ForeignKeyDef> foreignKeys = tableDef.getForeignKeys();
        if (foreignKeys.size() > 0) {

            for (ForeignKeyDef foreignKeyDef : foreignKeys) {

                TableDef foreignTable = foreignKeyDef.getForeignPrimaryKey().getTableDef();

                // Only if the table is in the list
                // or we take also the parent
                if (tableDefList.contains(foreignTable) || withForeignTable) {

                    addTableToVertex(foreignTable);

                    // Add Edge
                    g.addEdge(foreignTable, tableDef);

                }


            }

        }

    }



    private static Dag get() {
        return new Dag();
    }

    public static Dag get(TableDef tableDef) {
        return new Dag().addTable(tableDef);
    }

    public static Dag get(SchemaDef schema) {
        return get(schema.getTables());
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

    private Dag addTable(TableDef tableDef) {
        if (!this.tableDefList.contains(tableDef)) {
            this.tableDefList.add(tableDef);
        }
        return this;
    }

    /**
     * Return an ordered list of tables ready to be loaded
     *
     * @return
     */
    public List<TableDef> getCreateOrderedTables() {

        if (g == null) {
            build();
        }

        TableDef currentTableDef = null;
        TableDef previousTableDef = null;
        List<TableDef> tableDefs = new ArrayList<>();
        TopologicalOrderIterator<TableDef, DefaultEdge> orderIterator = new TopologicalOrderIterator<>(g);

        // Because of the algorithm, we may have a table with a parent between table without parent
        // Little trick with on slot
        while (orderIterator.hasNext()) {
            currentTableDef = orderIterator.next();

            if (currentTableDef.getForeignKeys().size() == 0) {
                tableDefs.add(currentTableDef);
            } else {
                if (previousTableDef != null) {
                    tableDefs.add(previousTableDef);
                    tableDefs.add(currentTableDef);
                    previousTableDef = null;
                } else {
                    previousTableDef = currentTableDef;
                }
            }
        }
        // Add the last one
        if (previousTableDef != null) {
            tableDefs.add(currentTableDef);
        }
        return tableDefs;
    }

    /**
     * Return an ordered list of tables ready to be dropped
     *
     * @return
     */
    public List<TableDef> getDropOrderedTables() {

        List<TableDef> tables = getCreateOrderedTables();
        return Lists.reverse(tables);
    }

}
