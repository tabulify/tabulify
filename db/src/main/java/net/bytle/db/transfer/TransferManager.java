package net.bytle.db.transfer;


import net.bytle.db.DbLoggers;
import net.bytle.db.Tabular;
import net.bytle.db.engine.ForeignKeyDag;
import net.bytle.db.memory.queue.MemoryQueueDataPath;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.log.Log;

import java.sql.Types;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * A class to transfer a tabular data document content from a data source to another
 * <p>
 * One source can have only one target otherwise this is too complicated
 */
public class TransferManager {

  public static final Log LOGGER = Log.getLog(TransferManager.class);

  /**
   * If the select stream can only be generated
   * after another, this select stream is dependent
   */
  private boolean withDependencies = false;

  private final List<Integer> typesNotSupported = Arrays.asList(
    Types.ARRAY,
    Types.BINARY,
    Types.BLOB,
    Types.CLOB,
    Types.BIT
  );


  private Map<DataPath, TransferSourceTarget> transfers = new HashMap<>();


  private TransferProperties transferProperties = TransferProperties.of();

  /**
   * An utility function to start only one transfer
   *
   * @param source
   * @param target
   * @param transferProperties
   * @return
   */
  public static TransferListener transfer(DataPath source, DataPath target, TransferProperties transferProperties) {
    return of().addTransfer(source, target).setTransferProperties(transferProperties).start().get(0);
  }

  public TransferManager setTransferProperties(TransferProperties transferProperties) {
    this.transferProperties = transferProperties;
    return this;
  }


  public List<TransferListener> dependantTransfer(Transfer transfer) {


    for (TransferSourceTarget transferSourceTarget : transfer.getSourceTargets()) {
      TransferManager.checkSource(transferSourceTarget.getSourceDataPath());
      TransferManager.createOrCheckTargetFromSource(transferSourceTarget.getSourceDataPath(), transferSourceTarget.getTargetDataPath());
    }

    List<TransferListener> transferListeners = new ArrayList<>();

    List<List<Object>> streamTransfers = new ArrayList<>();
    for (TransferSourceTarget transferSourceTarget : transfer.getSourceTargets()) {
      TransferListener transferListener = TransferListener.of(transferSourceTarget);
      transferListeners.add(transferListener);
      transferListener.startTimer();
      SelectStream sourceSelectStream = Tabulars.getSelectStream(transferSourceTarget.getSourceDataPath());
      InsertStream targetInsertStream = Tabulars.getInsertStream(transferSourceTarget.getTargetDataPath());
      List<Object> mapStream = new ArrayList<>();
      mapStream.add(sourceSelectStream);
      mapStream.add(targetInsertStream);
      streamTransfers.add(mapStream);
      transferListener.addInsertListener(targetInsertStream.getInsertStreamListener());
      transferListener.addSelectListener(sourceSelectStream.getSelectStreamListener());

    }


    boolean showMustGoOn = true;
    while (showMustGoOn) {
      showMustGoOn = false;
      for (int i = 0; i < streamTransfers.size(); i++) {

        SelectStream sourceSelectStream = (SelectStream) streamTransfers.get(i).get(0);
        Boolean next = sourceSelectStream.next();
        if (next) {
          showMustGoOn = true;
          InsertStream targetInsertStream = (InsertStream) streamTransfers.get(i).get(1);
          List<Object> objects = IntStream.range(0, sourceSelectStream.getDataPath().getOrCreateDataDef().getColumnsSize())
            .mapToObj(sourceSelectStream::getObject)
            .collect(Collectors.toList());
          targetInsertStream.insert(objects);
        }
      }
    }

    transferListeners.forEach(TransferListener::stopTimer);
    return transferListeners;
  }

  /**
   * Check a tabular source before moving
   *   * check if it exists (except for query)
   *   * check if it has a structure
   * @param sourceDataPath
   */
  public static void checkSource(DataPath sourceDataPath) {
    // Check source
    if (!Tabulars.exists(sourceDataPath)) {
      // Is it a query definition
      if (sourceDataPath.getQuery() == null) {
        throw new RuntimeException("We cannot move the source data path (" + sourceDataPath + ") because it does not exist");
      }
    }
    if (sourceDataPath.getOrCreateDataDef().getColumnDefs().length==0){
      throw new RuntimeException("We cannot move this tabular data path (" + sourceDataPath + ") because it has no columns.");
    }
  }

  private TransferListener atomicTransfer(Transfer transfer) {

    assert transfer.getSourceTargets().size() == 1 : "This is not a transfer of only one source/target";

    TransferSourceTarget transferSourceTarget = transfer.getSourceTargets().get(0);
    DataPath sourceDataPath = transferSourceTarget.getSourceDataPath();
    DataPath targetDataPath = transferSourceTarget.getTargetDataPath();
    TransferProperties transferProperties = transfer.getTransferProperties();

    // Check source
    TransferManager.checkSource(sourceDataPath);

    // Check Target
    TransferManager.createOrCheckTargetFromSource(sourceDataPath, targetDataPath);

    /**
     * The listener is passed to the consumers and producers threads
     * to ultimately ends in the view thread to report life on the process
     */
    TransferListener transferListener = TransferListener.of(transferSourceTarget);
    transferListener.startTimer();

    /**
     * Single thread ?
     */
    int targetWorkerCount = transfer.getTransferProperties().getTargetWorkerCount();
    if (targetWorkerCount == 1) {
      try (
        SelectStream sourceSelectStream = Tabulars.getSelectStream(sourceDataPath);
        InsertStream targetInsertStream = Tabulars.getInsertStream(targetDataPath)
      ) {

        transferListener.addInsertListener(targetInsertStream.getInsertStreamListener());
        transferListener.addSelectListener(sourceSelectStream.getSelectStreamListener());

        while (sourceSelectStream.next()) {
          List<Object> objects = IntStream.range(0, sourceSelectStream.getDataPath().getOrCreateDataDef().getColumnsSize())
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
    if (targetWorkerCount > targetDataPath.getDataStore().getMaxWriterConnection()) {
      throw new IllegalArgumentException("The database (" + targetDataPath.getDataStore().getName() + ") does not support more than (" + targetDataPath.getDataStore().getMaxWriterConnection() + ") connections. We can then not start (" + targetWorkerCount + ") workers. (1) connection is also in use.");
    }


    // Object flag status
    AtomicBoolean producerWorkIsDone = new AtomicBoolean(false);
    AtomicBoolean consumerWorkIsDone = new AtomicBoolean(false);


    // The queue between the producer (source) and the consumer (target)
    long timeout = transferProperties.getTimeOut();
    MemoryQueueDataPath queue = ((MemoryQueueDataPath) Tabular.tabular().getDataPath("Transfer"))
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
            transferSourceTarget,
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
      target.getOrCreateDataDef().copyDataDef(source);
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
    if (target.getOrCreateDataDef().getColumnsSize() != 0) {
      for (ColumnDef columnDef : source.getOrCreateDataDef().getColumnDefs()) {
        ColumnDef targetColumnDef = target.getOrCreateDataDef().getColumnDef(columnDef.getColumnName());
        if (targetColumnDef == null) {
          String message = "Unable to move the data unit (" + source.toString() + ") because it exists already in the target location (" + target.toString() + ") with a different structure" +
            " (The source column (" + columnDef.getColumnName() + ") was not found in the target data unit)";
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new RuntimeException(message);
        }
      }
    } else {
      target.getOrCreateDataDef().copyDataDef(source);
    }
  }


  public List<Transfer> getTransfersToBeExecuted() {
    List<DataPath> sourceDataPaths = new ArrayList<>(transfers.keySet());

    // Get the source datapath by child/parent orders
    List<DataPath> dagDataPaths = ForeignKeyDag
      .get(sourceDataPaths)
      .setWithDependency(this.withDependencies)
      .getCreateOrderedTables();

    // If this with dependencies, we may miss some transfer
    // we add them here
    if (this.withDependencies) {
      // The target is the first one defined
      DataPath target = transfers.values().iterator().next().getTargetDataPath();
      for (DataPath sourceDataPath : dagDataPaths) {
        TransferSourceTarget transferSourceTarget = transfers.get(sourceDataPath);
        if (transferSourceTarget == null) {
          if (Tabulars.isDocument(target)) {
            target = target.getSibling(sourceDataPath.getName());
          }
          transfers.put(sourceDataPath, TransferSourceTarget.of(sourceDataPath, target));
        }
      }
    }

    // Building the transfers that we are finally going to execute
    List<Transfer> finalTransfers = new ArrayList<>();
    for (DataPath dataPath : dagDataPaths) {
      DataPath selectStreamDependency = dataPath.getSelectStreamDependency();
      if (selectStreamDependency != null) {
        finalTransfers.stream().forEach(
          t -> {
            if (t.getSources().contains(selectStreamDependency)) {
              t.addSourceTargetDataPath(transfers.get(dataPath));
            }
          }
        );
      } else {
        finalTransfers.add(
          Transfer.of()
            .addSourceTargetDataPath(transfers.get(dataPath))
        );
      }
    }
    return finalTransfers;

  }


  public List<TransferListener> start() {

    List<Transfer> transfers = getTransfersToBeExecuted();
    List<TransferListener> transferListeners = new ArrayList<>();
    for (Transfer transfer : transfers) {
      if (transfer.sourceTargets.size() > 1) {
        transferListeners.addAll(dependantTransfer(transfer));
      } else {
        TransferListener transferlistener = atomicTransfer(transfer);
        transferListeners.add(transferlistener);
      }
    }
    return transferListeners;
  }

  public TransferManager() {
  }

  /**
   * If we try to load from a select stream that is dependent on another
   * the dependent select stream will also be loaded, created
   * otherwise an error is thrown
   *
   * @param b
   * @return
   */
  public TransferManager withDependency(boolean b) {
    this.withDependencies = b;
    return this;
  }

  public static TransferManager of() {
    return new TransferManager();
  }

  /**
   * @param source
   * @param target if the target is a container, the target will become a child of it with the name of the source
   * @return
   */
  public TransferManager addTransfer(DataPath source, DataPath target) {
    assert source!=null:"The source cannot be null in a transfer";
    assert target!=null:"The target cannot be null in a transfer";

    if (Tabulars.isContainer(target)) {
      String name = source.getName();
      if (name == null) { // Query case
        name = source.getDescription();
      }
      target = target.getChildAsTabular(name);
    }

    transfers.put(source, TransferSourceTarget.of(source, target));

    return this;
  }


  public TransferManager addTransfers(List<DataPath> sources, DataPath target) {
    sources.forEach(s -> this.addTransfer(s, target));
    return this;
  }
}





