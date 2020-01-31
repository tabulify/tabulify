package net.bytle.db.transfer;


import net.bytle.db.DbLoggers;
import net.bytle.db.engine.SelectStreamDag;
import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.DataDefs;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.log.Log;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.bytle.db.memory.MemoryDataPathType.TYPE_BLOCKED_QUEUE;


/**
 * A class to transfer a tabular data document content from a data source to another
 */
public class TransferManager {

  public static final Log LOGGER = Log.getLog(TransferManager.class);

  private final List<Integer> typesNotSupported = Arrays.asList(
    Types.ARRAY,
    Types.BINARY,
    Types.BLOB,
    Types.CLOB,
    Types.BIT
  );
  private List<Transfer> transfers = new ArrayList<>();
  List<TransferListener> transferListeners = new ArrayList<>();

  /**
   * An utility function to start only one transfer
   *
   * @param source
   * @param target
   * @param transferProperties
   * @return
   */
  public static TransferListener transfer(DataPath source, DataPath target, TransferProperties transferProperties) {
    return of().addTransfer(source, target, transferProperties).start().get(0);
  }


  public TransferListener childParentTransfer(List<Transfer> transfers) {

    for (Transfer transfer : transfers) {
      TransferManager.checkSource(transfer.getSourceDataPath());
      TransferManager.createOrCheckTargetFromSource(transfer.getSourceDataPath(), transfer.getTargetDataPath());
    }
    TransferListener transferListener = TransferListener.of();
    transferListener.startTimer();

    List<List<Object>> streamTransfers = new ArrayList<>();
    for (Transfer transfer : transfers) {
      SelectStream sourceSelectStream = Tabulars.getSelectStream(transfer.getSourceDataPath());
      InsertStream targetInsertStream = Tabulars.getInsertStream(transfer.getTargetDataPath());
      List<Object> mapStream = new ArrayList<>();
      mapStream.add(sourceSelectStream);
      mapStream.add(targetInsertStream);
      streamTransfers.add(mapStream);
      transferListener.addInsertListener(targetInsertStream.getInsertStreamListener());
      transferListener.addSelectListener(sourceSelectStream.getSelectStreamListener());
    }

    SelectStream firstStream = (SelectStream) streamTransfers.get(0).get(0);
    while (firstStream.next()) {
      for (int i = 0; i < streamTransfers.size(); i++) {

        SelectStream sourceSelectStream;
        if (i == 0) {
          sourceSelectStream = firstStream;
        } else {
          sourceSelectStream = (SelectStream) streamTransfers.get(i);
          sourceSelectStream.next();
        }
        InsertStream targetInsertStream = (InsertStream) streamTransfers.get(i);

        List<Object> objects = IntStream.range(0, sourceSelectStream.getSelectDataDef().getColumnDefs().size())
          .mapToObj(sourceSelectStream::getObject)
          .collect(Collectors.toList());
        targetInsertStream.insert(objects);
      }
    }

    transferListener.stopTimer();
    return transferListener;
  }

  public static void checkSource(DataPath sourceDataPath) {
    // Check source
    if (!Tabulars.exists(sourceDataPath)) {
      // Is it a query definition
      if (sourceDataPath.getQuery() == null) {
        throw new RuntimeException("We cannot move the source data path (" + sourceDataPath + ") because it does not exist");
      }
    }
  }

  public TransferListener transfer(Transfer transfer) {

    DataPath sourceDataPath = transfer.getSourceDataPath();
    DataPath targetDataPath = transfer.getTargetDataPath();
    TransferProperties transferProperties = transfer.getTransferProperties();

    // Check source
    TransferManager.checkSource(sourceDataPath);

    // Check Target
    TransferManager.createOrCheckTargetFromSource(sourceDataPath, targetDataPath);

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
          List<Object> objects = IntStream.range(0, sourceSelectStream.getSelectDataDef().getColumnDefs().size())
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
      Tabulars.copyDataDef(source, target);
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


  public List<TransferListener> start() {
    List<DataPath> dataPaths = transfers.stream()
      .map(Transfer::getSourceDataPath)
      .collect(Collectors.toList());
    List<DataPath> dataPath = SelectStreamDag.get(dataPaths).getCreateOrderedTables();

    return transferListeners;
  }

  public TransferManager() {
  }

  public static TransferManager of() {
    return new TransferManager();
  }

  public TransferManager addTransfer(DataPath source, DataPath target, TransferProperties transferProperties) {
    transfers.add(Transfer.of()
      .setSourceDataPath(source)
      .setTargetDataPath(target)
      .setTransferProperties(transferProperties));
    return this;
  }

}





