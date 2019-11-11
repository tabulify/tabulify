package net.bytle.db.tpc;

import com.teradata.tpcds.Options;
import com.teradata.tpcds.Session;
import com.teradata.tpcds.Table;
import com.teradata.tpcds.TableGenerator;
import net.bytle.log.Log;
import net.bytle.db.engine.Dag;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.stream.MoveListener;

import java.util.ArrayList;
import java.util.List;

public class TpcdsDgen {

    public static final Log LOGGER = Tpc.LOGGER_TPC;

    Options options = new Options();
    // Every 5 * 10 000 = 50 000 rows
    private Integer feedbackFrequency = 5;
    private TableSystem tableSystem;

    private TpcdsDgen() {

        options.overwrite = true;
        options.directory = "./target";

    }

    static public TpcdsDgen get() {

        return new TpcdsDgen();

    }



    public TpcdsDgen setChunkNumber(Integer n) {
        this.options.parallelism = n;
        return this;
    }

    public TpcdsDgen setSeparator(char separator) {
        this.options.separator = separator;
        return this;
    }

    public TpcdsDgen setTableSystem(TableSystem tableSystem) {
        this.tableSystem = tableSystem;
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
    public List<MoveListener> load(DataPath table) {
        List<DataPath> tables = new ArrayList<>();
        tables.add(table);
        return load(tables);
    }


    public List<MoveListener> load(List<DataPath> dataPaths) {

        Session session = options.toSession();

        if (dataPaths.size() == 1) {
            // session.generateOnlyOneTable()
            this.options.table = dataPaths.get(0).getName();
            LOGGER.info("Loading only one Tpcds table " + dataPaths.get(0).getName());
        } else {
            LOGGER.info("Loading " + dataPaths.size() + " Tpcds tables");
        }


        // Building the table to load
        dataPaths = Dag.get(dataPaths).getCreateOrderedTables();
        List<MoveListener> insertStreamListeners = new ArrayList<>();
        for (DataPath dataPath : dataPaths) {

            LOGGER.info("Loading the table " + dataPath.toString());
            List<Thread> threads = new ArrayList<>();

            for (int i = 1; i <= session.getParallelism(); i++) {

                int chunkNumber = i;

                Table table;
                if (!dataPath.getName().startsWith("s_")) {
                    table = Table.getTable(dataPath.getName());
                } else {
                    LOGGER.severe("The staging table are not yet supported");
                    continue;
//                    table = Table.getSourceTables()
//                            .stream()
//                            .filter(s -> s.getName().toLowerCase().equals(tableDef.getName().toLowerCase()))
//                            .collect(Collectors.toList())
//                            .of(0);
                }


                Thread thread;
                if (tableSystem == null) {
                    LOGGER.info("Generate the chunk " + chunkNumber + " for the table (" + dataPath.getName() + ") in a file");
                    thread = new Thread(() -> {

                        TableGenerator tableGenerator = new TableGenerator(session.withChunkNumber(chunkNumber));
                        tableGenerator.generateTable(table);

                    });
                } else {
                    LOGGER.fine("Loading the table (" + dataPath.getName() + ") with the " + chunkNumber + " thread");
                    //TODO: if there is an exception in the thread, it is not caught
                    thread = new Thread(() -> {
                        List<MoveListener> insertStreamListener=TpcdsDgenTable.get(session.withChunkNumber(chunkNumber), tableSystem)
                                    .setRowFeedback(feedbackFrequency)
                                    .generateTable(table);
                        if (insertStreamListener != null) {
                            insertStreamListeners.addAll(insertStreamListener);
                        }
                    });
                }
                final String threadId = "Table " + dataPath.getName() + ", chunk " + chunkNumber;
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
