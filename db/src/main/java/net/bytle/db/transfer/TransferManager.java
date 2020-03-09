package net.bytle.db.transfer;


import net.bytle.db.Tabular;
import net.bytle.db.engine.ForeignKeyDag;
import net.bytle.db.memory.queue.MemoryQueueDataPath;
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
 * A transfer manager.
 * <p>
 * * You add you transfer with {@link #addTransfer(DataPath, DataPath)} or {@link #addTransfers(List, DataPath)}
 * * You set your properties via {@link #getProperties()}
 * * You say if you also want to transfer the source foreign table via {@link #withDependency(boolean)}
 * * and you {@link #start()}
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


  // Transfers by source data path
  private Map<DataPath, TransferSourceTarget> transfers = new HashMap<>();


  private TransferProperties transferProperties = TransferProperties.of();

  /**
   * An utility function to start only one transfer
   *
   * @param source
   * @param target
   * @return
   */
  public static TransferListener transfer(DataPath source, DataPath target) {
    return of().addTransfer(source, target).start().get(0);
  }

  public TransferManager setTransferProperties(TransferProperties transferProperties) {
    this.transferProperties = transferProperties;
    return this;
  }


  /**
   * A dependent transfer is used when the data generation of a source
   * is dependent of another source
   * <p>
   * This was created originally for the loading of TPC table (the returns data
   * are generated at the same time that the sales data) but you could
   * easily imagine that in a tree format (Xml, ...), you first need to create
   * the parent to be able to create the child
   * <p>
   * If a source has no {@link DataPath#getSelectStreamDependency()}, the {@link #atomicTransfer(TransferSourceTarget)}
   * is normally used
   *
   * @param transferSourceTargets
   * @return
   */
  public List<TransferListener> dependantTransfer(List<TransferSourceTarget> transferSourceTargets) {


    for (TransferSourceTarget transferSourceTarget : transferSourceTargets) {
      transferSourceTarget.checkSource();
      transferSourceTarget.createOrCheckTargetFromSource();
    }

    List<TransferListener> transferListeners = new ArrayList<>();

    List<List<Object>> streamTransfers = new ArrayList<>();
    for (TransferSourceTarget transferSourceTarget : transferSourceTargets) {
      TransferListenerStream transferListenerStream = new TransferListenerStream(transferSourceTarget);
      transferListeners.add(transferListenerStream);
      transferListenerStream.startTimer();
      SelectStream sourceSelectStream = Tabulars.getSelectStream(transferSourceTarget.getSourceDataPath());
      InsertStream targetInsertStream = Tabulars.getInsertStream(transferSourceTarget.getTargetDataPath());
      List<Object> mapStream = new ArrayList<>();
      mapStream.add(sourceSelectStream);
      mapStream.add(targetInsertStream);
      streamTransfers.add(mapStream);
      transferListenerStream.addInsertListener(targetInsertStream.getInsertStreamListener());
      transferListenerStream.addSelectListener(sourceSelectStream.getSelectStreamListener());

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
   * Transfer of data from one source to one target
   * <p>
   * There is also a transfer from multiple source to one target
   * when the source generation is dependent called {@link #dependantTransfer(List)}
   * <p>
   * This function supports the loading of data with multiple threads (ie
   * when the {@link TransferProperties#setTargetWorkerCount(int)} is bigger than one)
   *
   * @param transferSourceTarget
   * @return
   */
  private TransferListener atomicTransfer(TransferSourceTarget transferSourceTarget) {


    DataPath sourceDataPath = transferSourceTarget.getSourceDataPath();
    DataPath targetDataPath = transferSourceTarget.getTargetDataPath();

    // Check source
    transferSourceTarget.checkSource();

    // Check Target
    transferSourceTarget.createOrCheckTargetFromSource();

    // Check Data Type
    transferSourceTarget.checkColumnMappingDataType();

    /**
     * The listener is passed to the consumers and producers threads
     * to ultimately ends in the view thread to report life on the process
     */
    TransferListenerStream transferListenerStream = new TransferListenerStream(transferSourceTarget);
    transferListenerStream.startTimer();

    /**
     * Single thread ?
     */
    int targetWorkerCount = getProperties().getTargetWorkerCount();
    if (targetWorkerCount == 1) {
      try (
        SelectStream sourceSelectStream = Tabulars.getSelectStream(sourceDataPath);
        InsertStream targetInsertStream = Tabulars.getInsertStream(targetDataPath)
      ) {

        transferListenerStream.addInsertListener(targetInsertStream.getInsertStreamListener());
        transferListenerStream.addSelectListener(sourceSelectStream.getSelectStreamListener());

        // Get the objects from the source in a target order
        List<Integer> sourceColumnPositionInTargetOrder = transferSourceTarget.getSourceColumnPositionInTargetOrder();

        // Run
        while (sourceSelectStream.next()) {
          List<Object> objects = sourceColumnPositionInTargetOrder
            .stream()
            .map(i -> sourceSelectStream.getObject(i - 1))
            .collect(Collectors.toList());
          targetInsertStream.insert(objects);
        }

      }
      transferListenerStream.stopTimer();
      return transferListenerStream;

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
      TransferSourceWorker transferSourceWorker = new TransferSourceWorker(sourceDataPath, queue, transferProperties, transferListenerStream);
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
      TransferMetricsViewer transferMetricsViewer = new TransferMetricsViewer(queue, transferProperties, transferListenerStream, producerWorkIsDone, consumerWorkIsDone);
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

    transferListenerStream.stopTimer();
    return transferListenerStream;

  }







  /**
   * This step process all transfers: ie
   * * add the data dependencies if any {@link #withDependency(boolean)} - ie foreign
   * * add the runtime dependencies on select stream dependency if any {@link DataPath#getSelectStreamDependency()}
   * <p>
   * This step is typically run before a {@link #start()}
   *
   * @return
   */
  public List<List<TransferSourceTarget>> processAndGetTransfersToBeExecuted() {

    // The transfer is driven by the source
    // You want to move/copy a data set from a source to a target
    // Why driven by source:
    //    * You could move a whole schema if you just would select to load the fact table with its dependencies
    //    * the runtime dependency is on the source
    List<DataPath> sourceDataPaths = new ArrayList<>(transfers.keySet());

    // Get the source data path by child/parent orders
    // Ie dim/child/foreign first, parent/fact last
    List<DataPath> dagSourceDataPaths = ForeignKeyDag
      .get(sourceDataPaths)
      .setWithDependency(this.withDependencies)
      .getCreateOrderedTables();

    // If dependencies were added in the step before,
    // we miss some transfer, we add them below
    if (this.withDependencies) {
      // The target is the first one defined
      DataPath target = transfers.values().iterator().next().getTargetDataPath();
      for (DataPath sourceDataPath : dagSourceDataPaths) {
        TransferSourceTarget transferSourceTarget = transfers.get(sourceDataPath);
        if (transferSourceTarget == null) {
          if (Tabulars.isDocument(target)) {
            target = target.getSibling(sourceDataPath.getName());
          }
          transfers.put(sourceDataPath, new TransferSourceTarget(sourceDataPath, target));
        }
      }
    }


    // Building the transfers that we are finally going to execute
    // Do we have a runtime dependency
    // A list of source that should be loaded together
    List<List<DataPath>> groupedSourceDataPath = new ArrayList<>();
    for (DataPath dataPath : dagSourceDataPaths) {
      DataPath selectStreamDependency = dataPath.getSelectStreamDependency();
      if (selectStreamDependency != null) {
        List<DataPath> source = groupedSourceDataPath
          .stream()
          .filter(l -> l.contains(dataPath))
          .findFirst()
          .orElse(null);

        if (source == null) {
          groupedSourceDataPath.add(Arrays.asList(dataPath));
        } else {
          source.add(dataPath);
        }
      } else {
        //noinspection ArraysAsListWithZeroOrOneArgument
        groupedSourceDataPath.add(Arrays.asList(dataPath));
      }
    }

    // Finally transform the grouped source data path into grouped transfer source
    List<List<TransferSourceTarget>> transfersSourceTargets = groupedSourceDataPath.stream()
      .map(ldp -> ldp.stream().map(dp -> transfers.get(dp)).collect(Collectors.toList()))
      .collect(Collectors.toList());

    return transfersSourceTargets;

  }


  public List<TransferListener> start() {

    // Process the transfer
    List<List<TransferSourceTarget>> transfers = processAndGetTransfersToBeExecuted();

    // Run
    List<TransferListener> transferListeners = new ArrayList<>();
    for (List<TransferSourceTarget> transfer : transfers) {
      if (transfer.size() > 1) {
        List<TransferListener> dependantTransferListeners = dependantTransfer(transfer);
        transferListeners.addAll(dependantTransferListeners);
      } else {
        TransferListener transferlistener = atomicTransfer(transfer.get(0));
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
    assert source != null : "The source cannot be null in a transfer";
    assert target != null : "The target cannot be null in a transfer";

    if (Tabulars.isContainer(target)) {
      String name = source.getName();
      if (name == null) { // Query case
        name = source.getDescription();
      }
      target = target.getChildAsTabular(name);
    }

    transfers.put(source, new TransferSourceTarget(source, target));

    return this;
  }


  public TransferManager addTransfers(List<DataPath> sources, DataPath target) {
    sources.forEach(s -> this.addTransfer(s, target));
    return this;
  }

  public TransferProperties getProperties() {
    return this.transferProperties;
  }

  public TransferManager addTransfer(TransferSourceTarget transferSourceTarget) {
    transfers.put(transferSourceTarget.getSourceDataPath(), transferSourceTarget);
    return this;
  }
}





