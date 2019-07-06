package net.bytle.db.tpc;

import com.teradata.tpcds.Options;
import com.teradata.tpcds.Session;
import com.teradata.tpcds.Table;
import com.teradata.tpcds.TableGenerator;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Dag;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.cli.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TpcdsDgen {

    public static final Log LOGGER = Tpc.LOGGER_TPC;

    Options options = new Options();
    private Database database;
    // Every 5 * 10 000 = 50 000 rows
    private Integer feedbackFrequency = 5;

    private TpcdsDgen() {

        options.overwrite = true;
        options.directory = "./target";

    }

    static public TpcdsDgen get() {

        return new TpcdsDgen();

    }


    public TpcdsDgen setDirectory(Path path) {
        this.options.directory = path.normalize().toAbsolutePath().toString();
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    public TpcdsDgen setChunkNumber(Integer n) {
        this.options.parallelism = n;
        return this;
    }

    public TpcdsDgen setSeparator(char separator) {
        this.options.separator = separator;
        return this;
    }

    public TpcdsDgen setDatabase(Database database) {
        this.database = database;
        return this;
    }

    /**
     * Volume of data to generate in GB (Default: 1)
     *
     * @param scale
     * @return tpcdsgen for chaining init
     */
    public TpcdsDgen setScale(Double scale) {
        this.options.scale = scale;
        return this;
    }


    /**
     * Load only one table
     *
     * @param table
     * @return
     */
    public List<InsertStreamListener> load(TableDef table) {
        ArrayList<TableDef> tables = new ArrayList<TableDef>();
        tables.add(table);
        return load(tables);
    }


    public List<InsertStreamListener> load(List<TableDef> tables) {

        Session session = options.toSession();

        if (tables.size() == 1) {
            // session.generateOnlyOneTable()
            this.options.table = tables.get(0).getName();
            LOGGER.info("Loading only one Tpcds table " + tables.get(0).getName());
        } else {
            LOGGER.info("Loading " + tables.size() + " Tpcds tables");
        }


        // Building the table to load
        tables = Dag.get(tables).getCreateOrderedTables();
        List<InsertStreamListener> insertStreamListeners = new ArrayList<>();
        for (TableDef tableDef : tables) {

            LOGGER.info("Loading the table " + tableDef.getFullyQualifiedName());
            List<Thread> threads = new ArrayList<>();

            for (int i = 1; i <= session.getParallelism(); i++) {

                int chunkNumber = i;

                Table table;
                if (!tableDef.getName().startsWith("s_")) {
                    table = Table.getTable(tableDef.getName());
                } else {
                    LOGGER.severe("The staging table are not yet supported");
                    continue;
//                    table = Table.getSourceTables()
//                            .stream()
//                            .filter(s -> s.getName().toLowerCase().equals(tableDef.getName().toLowerCase()))
//                            .collect(Collectors.toList())
//                            .get(0);
                }


                Thread thread;
                if (database == null) {
                    LOGGER.info("Generate the chunk " + chunkNumber + " for the table (" + tableDef.getName() + ") in a file");
                    thread = new Thread(() -> {

                        TableGenerator tableGenerator = new TableGenerator(session.withChunkNumber(chunkNumber));
                        tableGenerator.generateTable(table);

                    });
                } else {
                    LOGGER.fine("Loading the table (" + tableDef.getName() + ") with the " + chunkNumber + " thread");
                    //TODO: if there is an exception in the thread, it si not caucght
                    thread = new Thread(() -> {
                        List<InsertStreamListener> insertStreamListener=TpcdsDgenTable.get(session.withChunkNumber(chunkNumber), database)
                                    .setRowFeedback(feedbackFrequency)
                                    .generateTable(table);
                        if (insertStreamListener != null) {
                            insertStreamListeners.addAll(insertStreamListener);
                        }
                    });
                }
                final String threadId = "Table " + tableDef.getName() + ", chunk " + chunkNumber;
                thread.setName(threadId);
                threads.add(thread);
                thread.start();
                LOGGER.fine(thread.getName() + "thread started");
            }

            try {

                for (Thread thread : threads) {
                    LOGGER.fine("Waiting that the thread (" + thread.getName() + ") has finished.");
                    thread.join();
                    LOGGER.fine("Thread (" + thread.getName() + ") has finished.");
                }

            } catch (InterruptedException e) {

                throw new RuntimeException(e);

            }

        }

        return insertStreamListeners;

    }


    public TpcdsDgen setFeedbackFrequency(Integer rowNumber) {
        this.feedbackFrequency = rowNumber;
        return this;
    }


}
