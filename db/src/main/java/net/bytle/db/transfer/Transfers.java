package net.bytle.db.transfer;


import net.bytle.db.DbLoggers;
import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.DataDefs;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.log.Log;

import java.sql.Types;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.bytle.db.memory.MemoryDataPathType.TYPE_BLOCKED_QUEUE;


/**
 * A class to transfer a tabular data document content from a data source to another
 */
public class Transfers {

    public static final Log LOGGER = Log.getLog(Transfers.class);

    private final List<Integer> typesNotSupported = Arrays.asList(
            Types.ARRAY,
            Types.BINARY,
            Types.BLOB,
            Types.CLOB,
            Types.BIT
    );

    public static TransferListener transfer(Transfer transfer) {

        DataPath sourceDataPath = transfer.getSourceDataPath();
        DataPath targetDataPath = transfer.getTargetDataPath();
        TransferProperties transferProperties = transfer.getTransferProperties();

        // Check source
        if (!Tabulars.exists(sourceDataPath)) {
            // Is it a query definition
            if (sourceDataPath.getDataDef().getQuery() == null) {
                throw new RuntimeException("We cannot move the source data path (" + sourceDataPath + ") because it does not exist");
            }
        }
        /**
         * The listener is passed to the consumers and producers threads
         * to ultimately ends in the view thread to report life on the process
         */
        TransferListener transferListener = TransferListener.of();
        transferListener.startTimer();

        /**
         * Single thread ?
         */
        int targetWorkerCount = transferProperties.getTargetWorkerCount();
        if (targetWorkerCount == 1) {
            try (
                    SelectStream sourceSelectStream = Tabulars.getSelectStream(sourceDataPath);
                    InsertStream targetInsertStream = Tabulars.getInsertStream(targetDataPath)
            ) {

                transferListener.addInsertListener(targetInsertStream.getInsertStreamListener());
                transferListener.addSelectListener(sourceSelectStream.getSelectStreamListener());

                while (sourceSelectStream.next()) {
                    List<Object> objects = IntStream.range(0, sourceSelectStream.getDataDef().getColumnDefs().size())
                            .mapToObj(sourceSelectStream::getObject)
                            .collect(Collectors.toList());
                    targetInsertStream.insert(objects);
                }

            }
            transferListener.stopTimer();
            return transferListener;
        }

        /**
         * Not every database can make a lot of connection
         * We may use the last connection object for single connection database such as sqlite.
         *
         * Example:
         *     * their is already a connection through a select for instance
         *     * and that the database does not support multiple connection (such as Sqlite)
         **/
        // One connection is already used in the construction of the database
        if (targetWorkerCount > targetDataPath.getDataSystem().getMaxWriterConnection()) {
            throw new IllegalArgumentException("The database (" + targetDataPath.getDataSystem().getProductName() + ") does not support more than (" + targetDataPath.getDataSystem().getMaxWriterConnection() + ") connections. We can then not start (" + targetWorkerCount + ") workers. (1) connection is also in use.");
        }


        // Object flag status
        AtomicBoolean producerWorkIsDone = new AtomicBoolean(false);
        AtomicBoolean consumerWorkIsDone = new AtomicBoolean(false);


        // The queue between the producer (source) and the consumer (target)
        long timeout = transferProperties.getTimeOut();
        MemoryDataPath queue = MemoryDataPath.of("Transfer")
                .setType(TYPE_BLOCKED_QUEUE)
                .setTimeout(timeout)
                .setCapacity(transferProperties.getQueueSize());

        try {

            // Start the producer thread
            TransferSourceWorker transferSourceWorker = new TransferSourceWorker(sourceDataPath, queue, transferProperties, transferListener);
            Thread producer = new Thread(transferSourceWorker);
            producer.start();

            // Start the consumer / target threads
            ExecutorService targetWorkExecutor = Executors.newFixedThreadPool(targetWorkerCount);
            for (int i = 0; i < targetWorkerCount; i++) {

                targetWorkExecutor.execute(
                        new TransferTargetWorker(
                                queue,
                                targetDataPath,
                                transferProperties,
                                producerWorkIsDone)
                );

            }

            // Start the viewer
            TransferMetricsViewer transferMetricsViewer = new TransferMetricsViewer(queue, transferProperties, transferListener, producerWorkIsDone, consumerWorkIsDone);
            Thread viewer = new Thread(transferMetricsViewer);
            viewer.start();

            // Wait the producer
            producer.join(); // Waits for this thread to die.
            producerWorkIsDone.set(true);

            // Shut down the targetWorkExecutor Service
            targetWorkExecutor.shutdown();
            // And wait the termination
            try {
                boolean result = targetWorkExecutor.awaitTermination(timeout, TimeUnit.SECONDS);
                if (!result) {
                    throw new RuntimeException("The timeout of the consumers (" + timeout + " s) elapsed before termination");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // Send a signal to the viewer that the consumer work is done
            consumerWorkIsDone.set(true);

            // Wait the viewer
            viewer.join();


        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        transferListener.stopTimer();
        return transferListener;

    }

    /**
     * Before a copy/move operations the target
     * table should exist.
     * <p>
     * If the target table:
     * - does not exist, creates the target table from the source
     * - exist, control that the column definition is the same
     *
     * @param source the source data path
     * @param target the target data path
     */
    public static void createOrCheckTargetFromSource(DataPath source, DataPath target) {
        // Check target
        final Boolean exists = Tabulars.exists(target);
        if (!exists) {
            DataDefs.copy(source.getDataDef(), target.getDataDef());
            Tabulars.create(target);
        } else {
            checkOrCreateTargetStructureFromSource(source, target);
        }
    }

    /**
     * Check that the target has the same structure than the source.
     * Create it if it does not exist
     *
     * @param source the source data path
     * @param target the target data path
     */
    public static void checkOrCreateTargetStructureFromSource(DataPath source, DataPath target) {
        // If this for instance, the move of a file, the file may exist
        // but have no content and therefore no structure
        if (target.getDataDef().getColumnDefs().size() != 0) {
            for (ColumnDef columnDef : source.getDataDef().getColumnDefs()) {
                ColumnDef targetColumnDef = target.getDataDef().getColumnDef(columnDef.getColumnName());
                if (targetColumnDef == null) {
                    String message = "Unable to move the data unit (" + source.toString() + ") because it exists already in the target location (" + target.toString() + ") with a different structure" +
                            " (The source column (" + columnDef.getColumnName() + ") was not found in the target data unit)";
                    DbLoggers.LOGGER_DB_ENGINE.severe(message);
                    throw new RuntimeException(message);
                }
            }
        } else {
            DataDefs.copy(source.getDataDef(), target.getDataDef());
        }
    }

    public static List<TransferListener> transfers(List<Transfer> transfers) {
        List<TransferListener> transferListeners = new ArrayList<>();

        for (Transfer transfer : transfers) {

            transferListeners.add(transfer(transfer));

        }
        return transferListeners;
    }

    public static TransferListener transfer(DataPath source, DataPath target, TransferProperties transferProperties) {
        return transfer(Transfer.of()
                .setSourceDataPath(source)
                .setTargetDataPath(target)
                .setTransferProperties(transferProperties));
    }
}





