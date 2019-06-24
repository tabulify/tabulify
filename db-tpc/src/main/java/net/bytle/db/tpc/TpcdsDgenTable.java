package net.bytle.db.tpc;

import com.teradata.tpcds.Results;
import com.teradata.tpcds.Session;
import com.teradata.tpcds.Table;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamListener;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.teradata.tpcds.Results.constructResults;
import static java.util.Objects.requireNonNull;

public class TpcdsDgenTable {

    public static final Logger LOGGER = DbLoggers.LOGGER_DB_SAMPLE;

    private final Session session;
    private final Database database;
    private Integer rowFeedback = 1;

    private TpcdsDgenTable(Session session, Database database) {
        this.session = requireNonNull(session, "session is null");
        this.database = database;
    }

    static synchronized TpcdsDgenTable get(Session session, Database database) {

        return new TpcdsDgenTable(session, database);

    }

    public List<InsertStreamListener> generateTable(Table table) {

        List<InsertStreamListener> insertStreamListeners = new ArrayList<>();

        try {

            // If this is a child table and not the only table being generated, it will be generated when its parent is generated, so move on.
            if (table.isChild() && !session.generateOnlyOneTable()) {
                return null;
            }


            TableDef parentTableDef = database.getTable(table.getName());
            // The table exist ?
            if (!Tables.exists(parentTableDef)) {
                throw new RuntimeException("The table (" + parentTableDef.getFullyQualifiedName() + ") does not exist");
            }
            Integer batchSize = 10000;
            InsertStream parentInsertStream =
                    Tables.getTableInsertStream(parentTableDef)
                            .setFeedbackFrequency(rowFeedback)
                            .setBatchSize(batchSize);

            InsertStream childInsertStream = null;
            if (table.hasChild()) {

                TableDef childTableDef = database.getTable(table.getChild().getName());
                // The table exist ?
                if (!Tables.exists(childTableDef)) {
                    throw new RuntimeException("The child  table (" + childTableDef.getFullyQualifiedName() + ") of the table (" + parentTableDef.getFullyQualifiedName() + ") does not exist in the database.");
                }
                childInsertStream = Tables.getTableInsertStream(childTableDef)
                        .setFeedbackFrequency(rowFeedback);
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
                        // writeResults(childWriter, parentAndChildRows.get(1));
                        List<String> childValues = parentAndChildRows.get(1);
                        if (childInsertStream.flushAtNextInsert()) {
                            parentInsertStream.flush();
                        }
                        childInsertStream.insert(childValues);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                // Close
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
