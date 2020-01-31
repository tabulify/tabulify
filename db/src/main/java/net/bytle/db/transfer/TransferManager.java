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
import java.util.*;
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

  /**
   * If the select stream can only be generated
   * after another, this select stream is dependent
   */
  private boolean withSelectStreamDependencies = false;

  private final List<Integer> typesNotSupported = Arrays.asList(
    Types.ARRAY,
    Types.BINARY,
    Types.BLOB,
    Types.CLOB,
    Types.BIT
  );

  /**
   * A map between the source data path and the transfer
   * This is an utility structure to :
   *   * retrieve the transfer by data path source
   *   * and makes sure that there is only one transfer by source
   */
  private Map<DataPath, Transfer> transfers = new HashMap<>();

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


  public TransferListener dependantTransfer(List<Transfer> transfers) {

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

    boolean showMustGoOn = true;
    while (showMustGoOn) {
      showMustGoOn = false;
      for (int i = 0; i < streamTransfers.size(); i++) {

        SelectStream sourceSelectStream = (SelectStream) streamTransfers.get(i).get(0);
        Boolean next = sourceSelectStream.next();
        if (next){
          showMustGoOn = true;
          InsertStream targetInsertStream = (InsertStream) streamTransfers.get(i).get(1);
          List<Object> objects = IntStream.range(0, sourceSelectStream.getSelectDataDef().getColumnDefs().size())
            .mapToObj(sourceSelectStream::getObject)
            .collect(Collectors.toList());
          targetInsertStream.insert(objects);
        }
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

  public TransferListener atomicTransfer(Transfer transfer) {

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

    List<DataPath> sourceDataPaths = transfers.values().stream()
      .map(Transfer::getSourceDataPath)
      .collect(Collectors.toList());

    // Get the source datapath by child/parent orders
    List<DataPath> dagDataPaths = SelectStreamDag
      .get(sourceDataPaths)
      .setWithDependency(this.withSelectStreamDependencies)
      .getDropOrderedTables();

    Set<DataPath> sourceDataPathsProcessed = new HashSet<>();

    for (DataPath dataPath: dagDataPaths){
      if (!sourceDataPathsProcessed.contains(dataPath)) {
        List<DataPath> selectStreamDependencies = dataPath.getSelectStreamDependencies();
        if (selectStreamDependencies.size() == 0) {
          sourceDataPathsProcessed.add(dataPath);
          transferListeners.add(atomicTransfer(transfers.get(dataPath)));
        } else {

          // Parent (Dependency) first
          List<DataPath> dependentDataPaths = new ArrayList<>();
          dependentDataPaths.addAll(selectStreamDependencies);
          dependentDataPaths.add(dataPath);
          sourceDataPathsProcessed.addAll(dependentDataPaths);

          // Transfer
          List<Transfer> sourceTransfers = dependentDataPaths
            .stream()
            .map(d-> {
              Transfer transfer = transfers.get(d);
              // The case if the parent/dependency transfer was not added
              if (transfer==null){
                Transfer childTransfer = transfers.get(dataPath);
                DataPath childTarget = childTransfer.getTargetDataPath();
                DataPath target;
                if (Tabulars.isDocument(childTarget)) {
                  target = childTarget.getSibling(d.getName());
                } else {
                  target = childTarget;
                }
                transfer = Transfer.of()
                  .setSourceDataPath(d)
                  .setTargetDataPath(target)
                  .setTransferProperties(childTransfer.getTransferProperties());
              }
              return transfer;
            })
            .collect(Collectors.toList());
          transferListeners.add(dependantTransfer(sourceTransfers));

        }
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
   * @param b
   * @return
   */
  public TransferManager withSelectStreamDependency(boolean b) {
    this.withSelectStreamDependencies = b;
    return this;
  }

  public static TransferManager of() {
    return new TransferManager();
  }

  /**
   *
   * @param source
   * @param target if the target is a container, the target will become a child of it with the name of the source
   * @param transferProperties
   * @return
   */
  public TransferManager addTransfer(DataPath source, DataPath target, TransferProperties transferProperties) {

    if (Tabulars.isContainer(target)){
      target = target.getChild(source.getName());
    }

    Transfer transfer = Transfer.of()
      .setSourceDataPath(source)
      .setTargetDataPath(target);
    if (transferProperties!=null){
      transfer.setTransferProperties(transferProperties);
    }
    transfers.put(source,transfer);
    return this;
  }

  public TransferManager addTransfer(DataPath source, DataPath target) {
    addTransfer(source,target,null);
    return this;
  }

  public TransferManager addTransfers(List<DataPath> sources, DataPath target) {
    sources.forEach(s->this.addTransfer(s,target));
    return this;
  }
}





