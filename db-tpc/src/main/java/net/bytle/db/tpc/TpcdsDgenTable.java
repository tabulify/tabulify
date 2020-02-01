package net.bytle.db.tpc;

import com.teradata.tpcds.Results;
import com.teradata.tpcds.Session;
import com.teradata.tpcds.Table;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.log.Log;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A class that is used with {@link TpcdsDgen}
 * that represents a table
 */
public class TpcdsDgenTable {

    public static final Log LOGGER = Tpc.LOGGER_TPC;

    private final Session session;
    private final TableSystem tableSystem;
    private Integer rowFeedback = 1;

    private TpcdsDgenTable(Session session, TableSystem tableSystem) {
        this.session = requireNonNull(session, "session is null");
        this.tableSystem = tableSystem;
    }

    static synchronized TpcdsDgenTable get(Session session, TableSystem schemaDef) {

        return new TpcdsDgenTable(session, schemaDef);

    }

    public List<InsertStreamListener> generateTable(Table table) {

        List<InsertStreamListener> insertStreamListeners = new ArrayList<>();

        try {

            // If this is a child table and not the only table being generated, it will be generated when its parent is generated, so move on.
            if (table.isChild() && !session.generateOnlyOneTable()) {
                return null;
            }


            DataPath parentTableDef = tableSystem.getDataPath(table.getName());
            // The table exist ?
            if (!Tabulars.exists(parentTableDef)) {
                throw new RuntimeException("The table (" + parentTableDef + ") does not exist");
            }
            Integer batchSize = 10000;

            // TODO: Need to be able to set an option to not go back to auto-commit when closing
            // Otherwise the second thread (the child will got in trouble)
            InsertStream parentInsertStream =
                    Tabulars.getInsertStream(parentTableDef)
                            .setFeedbackFrequency(rowFeedback)
                            .setBatchSize(batchSize);

            InsertStream childInsertStream = null;
            if (table.hasChild()) {

                DataPath childTableDef = tableSystem.getDataPath(table.getChild().getName());
                // The table exist ?
                if (!Tabulars.exists(childTableDef)) {
                    // The child  table ("store_returns") of the table ("store_sales") does not exist in the database.
                    // Is normal when you are not taking the store returns schema
                    LOGGER.warning("The child  table (" + childTableDef + ") of the table (" + parentTableDef + ") does not exist in the database.");
                } else {
                    childInsertStream = Tabulars.getInsertStream(childTableDef)
                            .setFeedbackFrequency(rowFeedback);
                }
            }
            // OutputStreamWriter parentWriter = addFileWriterForTable(table);
            // OutputStreamWriter childWriter = table.hasChild() && !session.generateOnlyOneTable() ? addFileWriterForTable(table.getChild()) : null)
            Results results = Results.constructResults(table, session);
            try {

                for (List<List<String>> parentAndChildRows : results) {
                    if (parentAndChildRows.size() > 0) {
                        final List<String> values = parentAndChildRows.get(0);
                        parentInsertStream.insert(values);
                    }
                    if (parentAndChildRows.size() > 1) {
                        // requireNonNull(childWriter, "childWriter is null, but a child row was produced");
                        // writeResults(childWriter, parentAndChildRows.of(1));
                        // A child insert stream may be null when the child is not part of the schema
                        // Example: The child  table ("store_returns") of the table ("store_sales") does not exist in the database.
                        if (childInsertStream!=null) {
                            List<String> childValues = parentAndChildRows.get(1);
                            if (childInsertStream.flushAtNextInsert()) {
                                parentInsertStream.flush();
                            }
                            childInsertStream.insert(childValues);
                        }
                    }
                }
            } catch (Exception e) {

                throw new RuntimeException(e);

            } finally {
                // We flush first because the closing will set autocommit to true

                parentInsertStream.flush();
                if (childInsertStream != null) {
                    childInsertStream.flush();
                }

                parentInsertStream.close();
                if (childInsertStream != null) {
                    childInsertStream.close();
                }
            }

            insertStreamListeners.add(parentInsertStream.getInsertStreamListener());
            if (childInsertStream != null) {
                insertStreamListeners.add(childInsertStream.getInsertStreamListener());
            }

        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            throw new RuntimeException(e);
        }

        return insertStreamListeners;

    }


    public TpcdsDgenTable setRowFeedback(Integer rowFeedback) {
        this.rowFeedback = rowFeedback;
        return this;
    }
}
