package net.bytle.db.transfer;


import net.bytle.db.Tabular;
import net.bytle.db.connection.Connection;
import net.bytle.db.engine.ForeignKeyDag;
import net.bytle.db.memory.MemoryDataPathType;
import net.bytle.db.memory.queue.MemoryQueueDataPath;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSystem;
import net.bytle.db.spi.SelectException;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.exception.NotFoundException;
import net.bytle.log.Log;
import net.bytle.log.Logs;
import net.bytle.type.Casts;

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
 * * You set your properties via {@link #getTransferProperties()}
 * * You say if you also want to transfer the source foreign table via {@link #withDependency(boolean)}
 * * and you {@link #run()}
 */
public class TransferManager {

  public static final Log LOGGER = Logs.createFromClazz(TransferManager.class);

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


  /**
   * The transfer properties for all transfers
   * on a {@link TransferSourceTarget}
   */
  private TransferProperties transferProperties = TransferProperties.create();
  private final List<TransferListener> transferListeners = new ArrayList<>();
  private boolean hasRun = false;

  /**
   * A list of the operation where the target operation have already run against
   * the target
   * Ie if we have several transfer with the same target
   * We create, truncate the target only once (not for each transfer)
   */
  private final List<DataPath> targetsWhereTargetOperationHasAlreadyRun = new ArrayList<>();


  /**
   * An utility function to start only one transfer
   *
   * @param source - the source
   * @param target - the target
   * @return the transfer listener
   */
  public static TransferListener transfer(DataPath source, DataPath target) {
    TransferManager start = create().addTransfer(source, target).run();
    return start.getTransferListeners().get(0);
  }

  public List<TransferListener> getTransferListeners() {
    return this.transferListeners;
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
   * If a source has no {@link DataPath#getSelectStreamDependency()}, the {@link #runCrossTransfer(TransferSourceTarget)}
   * is normally used
   *
   * @param transferSourceTargets - the source target transfer
   * @return the listener of the transfer
   */
  public List<TransferListener> runDependantTransfer(List<TransferSourceTarget> transferSourceTargets) {


    List<TransferListener> transferListeners = new ArrayList<>();

    List<List<Object>> streamTransfers = new ArrayList<>();
    for (TransferSourceTarget transferSourceTarget : transferSourceTargets) {
      /*
       * Listeners
       */
      TransferListenerStream transferListenerStream = new TransferListenerStream(transferSourceTarget);
      transferListeners.add(transferListenerStream);
      transferListenerStream.startTimer();
      /*
       * Source / Target Operations
       */
      transferSourceTarget.sourcePreChecks();

      /**
       * Target operation run only once by target
       */
      if (
        !targetsWhereTargetOperationHasAlreadyRun.contains(transferSourceTarget.getTargetDataPath())
      ) {
        transferSourceTarget.targetPreOperationsAndCheck(transferListenerStream, true);
        targetsWhereTargetOperationHasAlreadyRun.add(transferSourceTarget.getTargetDataPath());
      }


      /**
       * Yolo !
       */
      SelectStream sourceSelectStream;
      try {
        sourceSelectStream = transferSourceTarget.getSourceDataPath().getSelectStream();
      } catch (SelectException e) {
        throw new RuntimeException(e);
      }
      InsertStream targetInsertStream = transferSourceTarget.getTargetDataPath().getInsertStream(
        transferSourceTarget.getSourceDataPath(),
        transferSourceTarget.getTransferProperties()
      );
      List<Object> mapStream = new ArrayList<>();
      mapStream.add(sourceSelectStream);
      mapStream.add(targetInsertStream);
      streamTransfers.add(mapStream);
      transferListenerStream.addInsertListener(targetInsertStream.getInsertStreamListener());

    }


    boolean showMustGoOn = true;
    while (showMustGoOn) {
      showMustGoOn = false;
      for (List<Object> streamTransfer : streamTransfers) {

        SelectStream sourceSelectStream = (SelectStream) streamTransfer.get(0);
        boolean next = sourceSelectStream.next();
        if (next) {
          showMustGoOn = true;
          InsertStream targetInsertStream = (InsertStream) streamTransfer.get(1);
          List<Object> objects = IntStream.range(0, sourceSelectStream.getDataPath().getOrCreateRelationDef().getColumnsSize())
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
   * Transfer of a data resource from:
   * * one data source to another
   * * row by row
   * <p>
   * There is also a transfer from multiple source to one target
   * when the source generation is dependent called {@link #runDependantTransfer(List)}
   * <p>
   * This function supports the loading of data with multiple threads (ie
   * when the {@link TransferProperties#setTargetWorkerCount(int)} is bigger than one)
   *
   * @param transferSourceTarget - the source target transfer to execute
   * @return the result
   */
  private List<TransferListener> runCrossTransfer(TransferSourceTarget transferSourceTarget) {


    DataPath sourceDataPath = transferSourceTarget.getSourceDataPath();
    DataPath targetDataPath = transferSourceTarget.getTargetDataPath();
    TransferProperties transferProperties = transferSourceTarget.getTransferProperties();

    /*
     * Check load operation
     * The load operation is important during the
     * check of target structure
     * (ie a {@link TransferOperation#MOVE}
     * and {@link TransferOperation#COPY} operations
     * require the same data structure between the target and the source
     */
    if (transferProperties.getOperation() == null) {
      transferProperties.setOperation(TransferOperation.INSERT);
    }

    /*
     * The listener is passed to the consumers and producers threads
     * to ultimately ends in the view thread to report life on the process
     */
    TransferListenerStream transferListenerStream = new TransferListenerStream(transferSourceTarget);
    transferListenerStream.setType(TransferType.SINGLE_CROSS);
    transferListenerStream.startTimer();

    /*
     * Preload checks
     */
    // Check source
    transferSourceTarget.sourcePreChecks();
    // Check Target
    targetPreOperationControl(transferSourceTarget);
    transferSourceTarget.targetPreOperationsAndCheck(transferListenerStream, true);


    /*
     * Single thread ?
     */
    int targetWorkerCount = getTransferProperties().getTargetWorkerCount();
    if (targetWorkerCount == 1) {
      try (
        SelectStream sourceSelectStream = sourceDataPath.getSelectStream();
        InsertStream targetInsertStream = targetDataPath.getInsertStream(sourceDataPath, transferProperties)
      ) {

        transferListenerStream.setMethod(targetInsertStream.getMethod());
        transferListenerStream.addInsertListener(targetInsertStream.getInsertStreamListener());

        List<Integer> sourceColumnPositionsInOrder = transferSourceTarget
          .getTransferColumnMapping()
          .keySet()
          .stream()
          .sorted()
          .collect(Collectors.toList());
        // Run
        while (sourceSelectStream.next()) {
          List<Object> objects = new ArrayList<>();
          for (Integer sourceColumnPosition : sourceColumnPositionsInOrder ) {
            Object object = sourceSelectStream.getObject(sourceColumnPosition);
            objects.add(object);
          }
          targetInsertStream.insert(objects);
        }

      } catch (SelectException e) {
        throw new RuntimeException(e);
      }
      transferListenerStream.stopTimer();
      return Collections.singletonList(transferListenerStream);

    } else {

      /*
       * Not every database can make a lot of connection
       * We may use the last connection object for single connection database such as sqlite.
       *
       * Example:
       *     * there is already a connection through a select for instance
       *     * and that the database does not support multiple connection (such as Sqlite)
       **/
      // One connection is already used in the construction of the database
      if (targetWorkerCount > targetDataPath.getConnection().getMetadata().getMaxWriterConnection()) {
        throw new IllegalArgumentException("The database (" + targetDataPath.getConnection().getName() + ") does not support more than (" + targetDataPath.getConnection().getMetadata().getMaxWriterConnection() + ") connections. We can then not start (" + targetWorkerCount + ") workers. (1) connection is also in use.");
      }


      // Object flag status
      AtomicBoolean producerWorkIsDone = new AtomicBoolean(false);
      AtomicBoolean consumerWorkIsDone = new AtomicBoolean(false);


      // The queue between the producer (source) and the consumer (target)
      int timeout = transferProperties.getTimeOut();
      Tabular tabular = sourceDataPath.getConnection().getTabular();
      MemoryQueueDataPath buffer = (MemoryQueueDataPath) ((MemoryQueueDataPath) tabular.getMemoryDataStore()
        .getTypedDataPath(MemoryDataPathType.QUEUE, "buffer"))
        .setTimeout(timeout)
        .setCapacity(transferProperties.getBufferSize())
        .getOrCreateRelationDef()
        .mergeStruct(sourceDataPath)
        .getDataPath();
      Tabulars.create(buffer);

      try {

        // Start the viewer
        TransferWorkerMetricsViewer transferWorkerMetricsViewer = new TransferWorkerMetricsViewer(buffer, transferProperties, producerWorkIsDone, consumerWorkIsDone);
        Thread viewer = new Thread(transferWorkerMetricsViewer);
        viewer.start();

        // Start the producer thread
        TransferWorkerProducer transferWorkerProducer = new TransferWorkerProducer(
          TransferSourceTarget.create(sourceDataPath, buffer, transferProperties),
          transferWorkerMetricsViewer
        );
        Thread producer = new Thread(transferWorkerProducer);
        producer.start();

        // Start the consumer / target threads
        ExecutorService targetWorkExecutor = Executors.newFixedThreadPool(targetWorkerCount);
        for (int i = 0; i < targetWorkerCount; i++) {

          targetWorkExecutor.execute(
            new TransferWorkerConsumer(
              TransferSourceTarget.create(buffer, targetDataPath, transferProperties),
              producerWorkIsDone,
              transferWorkerMetricsViewer)
          );
        }


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

        Tabulars.drop(buffer);
        return Casts.castToListSafe(
          transferWorkerMetricsViewer.getListenersStream(),
          TransferListener.class
        );

      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }


    }


  }


  /**
   * This step process all transfers: ie
   * * add the data dependencies if any {@link #withDependency(boolean)} - ie foreign
   * * add the runtime dependencies on select stream dependency if any {@link DataPath#getSelectStreamDependency()}
   * <p>
   * This step is typically run before a {@link #run()}
   *
   * @return the source transfer to execute
   */
  public List<List<TransferSourceTarget>> processAndGetTransfersToBeExecuted() {

    /**
     * Be sure to have the same java object for the same
     * logical target
     *
     * In a batch transfer, we create the target structure (column)
     * only on the first transfer
     * See {@link #targetsWhereTargetOperationHasAlreadyRun}
     */
    Map<String, DataPath> uniqueTargets = transfers.values()
      .stream()
      .collect(Collectors.toMap(
        e -> e.getTargetDataPath().getLogicalName(),
        TransferSourceTarget::getTargetDataPath,
        (duplicate1, duplicate2) -> duplicate1)
      );

    transfers = transfers
      .entrySet()
      .stream()
      .map(e -> {
        TransferSourceTarget transferSourceTarget = e.getValue();
        String targetLogicalName = transferSourceTarget.getTargetDataPath().getLogicalName();
        DataPath targetDataPath = uniqueTargets.get(targetLogicalName);
        return new TransferSourceTarget(e.getKey(), targetDataPath, transferSourceTarget.getTransferProperties());
      })
      .collect(Collectors.toMap(
        TransferSourceTarget::getSourceDataPath,
        tst -> tst
      ));

    /**
     *
     * The transfer is driven by the source
     * You want to move/copy a data set from a source to a target
     * Why driven by source:
     *    * You could move a whole schema if you just would select to load the fact table with its dependencies
     *    * the runtime dependency is on the source
     */

    List<DataPath> sourceDataPaths = new ArrayList<>(transfers.keySet());

    /**
     * Select the source
     * Get the source data path by child/parent orders
     * Ie dim/child/foreign first, parent/fact last
     */
    List<DataPath> dagSourceDataPaths = ForeignKeyDag
      .createFromPaths(sourceDataPaths)
      .setWithDependency(this.withDependencies)
      .getCreateOrdered();

    /**
     * Adding transfer for dependencies if any
     *
     * If dependencies were added in the step before,
     * we miss some transfer, we add them below
     */
    if (this.withDependencies) {
      // The target is the first one defined
      DataPath target = transfers.values().iterator().next().getTargetDataPath();
      for (DataPath sourceDataPath : dagSourceDataPaths) {
        TransferSourceTarget transferSourceTarget = transfers.get(sourceDataPath);
        if (transferSourceTarget == null) {
          if (Tabulars.isDocument(target)) {
            target = target.getSibling(sourceDataPath.getName());
          }
          transfers.put(sourceDataPath, new TransferSourceTarget(sourceDataPath, target, this.transferProperties));
        }
      }
    }

    /**
     * Building the transfers that we are finally going to execute
     * Do we have a runtime dependency (stream)
     * A list of source that should be loaded together
     */
    List<List<DataPath>> groupedSourceDataPath = new ArrayList<>();
    for (DataPath dataPath : dagSourceDataPaths) {

      boolean dependency;
      try {
        dataPath.getSelectStreamDependency();
        dependency = true;
      } catch (NotFoundException e) {
        dependency = false;
      }

      if (dependency) {
        /**
         * Do we have already the source in the grouped source
         */
        List<DataPath> sourceInGroupedSource = groupedSourceDataPath
          .stream()
          .filter(l -> l.contains(dataPath))
          .findFirst()
          .orElse(null);

        /**
         * If the source is not in grouped path create it
         * otherwise add it
         */
        if (sourceInGroupedSource == null) {
          groupedSourceDataPath.add(Collections.singletonList(dataPath));
        } else {
          sourceInGroupedSource.add(dataPath);
        }

      } else {

        groupedSourceDataPath.add(Collections.singletonList(dataPath));

      }

    }

    /**
     * Finally transform the grouped source data path into grouped transfer source
     * and return
     */
    return groupedSourceDataPath
      .stream()
      .map(ldp -> ldp.stream()
        .map(transfers::get)
        .collect(Collectors.toList())
      )
      .collect(Collectors.toList());

  }


  public TransferManager run() {

    // Circuit breaker
    if (this.hasRun) {
      throw new IllegalStateException("This manager has already ran, you can't run it again");
    }
    this.hasRun = true;

    // Process the transfer
    List<List<TransferSourceTarget>> transfers = processAndGetTransfersToBeExecuted();

    // Before

    // Run
    for (List<TransferSourceTarget> listTransferSourceTarget : transfers) {
      if (listTransferSourceTarget.size() > 1) {

        List<TransferListener> dependantTransferListeners = runDependantTransfer(listTransferSourceTarget);
        transferListeners.addAll(dependantTransferListeners);

      } else {

        TransferSourceTarget firstTransferSourceTarget = listTransferSourceTarget.get(0);

        /*
         * MetaMove or Not
         * ie move done by:
         * * the data system
         *    * file (copy/insert/)
         *    * database (create sql, ...)
         * * or not (row by row)
         *
         * Special CSV Case: when the format has already the header in the content
         * the meta move on file system cannot be done otherwise the headers will get there 2 times
         */
        boolean metaMove = false;
        boolean headerInContent = firstTransferSourceTarget.getSourceDataPath().getMediaType().getSubType().equals("csv");
        Boolean targetExist = Tabulars.exists(firstTransferSourceTarget.getTargetDataPath());
        boolean sameDataStore = sameDataStore(firstTransferSourceTarget);
        if(sameDataStore && !targetExist){
          metaMove = true;
        }
        if(sameDataStore && targetExist && !headerInContent){
          metaMove = true;
        }

        if (metaMove) {

          // same provider (fs or jdbc)
          Connection connection = firstTransferSourceTarget.getSourceDataPath().getConnection();
          final DataSystem sourceDataSystem = connection.getDataSystem();
          try {

            // Check Target
            targetPreOperationControl(firstTransferSourceTarget);
            TransferListener transferListener = sourceDataSystem.transfer(
              firstTransferSourceTarget.getSourceDataPath(),
              firstTransferSourceTarget.getTargetDataPath(),
              firstTransferSourceTarget.getTransferProperties()
            );
            transferListeners.add(transferListener);

          } catch (UnsupportedOperationException e) {

            /*
             * The operation may be not implemented
             *
             * Example: {@link TransferOperation#UPSERT} may be only implemented at the record level
             * and not at a set level
             */

            if (Tabulars.exists(firstTransferSourceTarget.getTargetDataPath())) {
              // example: case of a move (ie copy + delete)
              // the copy is successful but the `delete` is not
              throw new UnsupportedOperationException("The target file was created but there was an unsupported operation. Error: " + e.getMessage(), e);
            } else {
              String msg = "The system (" + sourceDataSystem + ") of the data store (" + connection + ") does not support the (" + firstTransferSourceTarget.getTransferProperties().getOperation() + ") transfer operation locally, we are performing it with cross data transfer. Error: " + e.getMessage();
              LOGGER.warning(msg);
              transferListeners.addAll(runCrossTransfer(firstTransferSourceTarget));
            }
          }

        } else {

          transferListeners.addAll(runCrossTransfer(firstTransferSourceTarget));

        }

        /*
         * Do we need to drop the source
         */
        if (
          this.transferProperties.transferSourceOperations.contains(TransferResourceOperations.DROP)
            || this.transferProperties.getOperation() == TransferOperation.MOVE
        ) {
          if (Tabulars.exists(firstTransferSourceTarget.getSourceDataPath())) {
            Tabulars.drop(firstTransferSourceTarget.getSourceDataPath());
          }
        }
      }
    }
    return this;
  }

  /**
   * The target pre operation should run only once
   * by batch and by target, this method controls this behaviour
   *
   * @param transferSourceTarget - a transfer source target to control
   */
  private void targetPreOperationControl(TransferSourceTarget transferSourceTarget) {
    if (
      !targetsWhereTargetOperationHasAlreadyRun.contains(transferSourceTarget.getTargetDataPath())
    ) {
      transferSourceTarget.getTransferProperties().setRunPreDataOperation(true);
      targetsWhereTargetOperationHasAlreadyRun.add(transferSourceTarget.getTargetDataPath());
    } else {
      transferSourceTarget.getTransferProperties().setRunPreDataOperation(false);
    }
  }


  private boolean sameDataStore(TransferSourceTarget transferSourceTarget) {
    return transferSourceTarget.getSourceDataPath().getConnection().getServiceId().equals(transferSourceTarget.getTargetDataPath().getConnection().getServiceId());
  }

  public TransferManager() {
  }

  /**
   * If we try to load from a select stream that is dependent on another
   * the dependent select stream will also be loaded, created
   * otherwise an error is thrown
   *
   * @param b - with or without dependency
   * @return the object for chaining
   */
  public TransferManager withDependency(boolean b) {
    this.withDependencies = b;
    return this;
  }

  public static TransferManager create() {
    return new TransferManager();
  }

  /**
   * @param source - the source of the transfer
   * @param target - if the target is a container, the target will become a child of it with the name of the source
   * @return the object for chaining
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

    transfers.put(source, new TransferSourceTarget(source, target, this.transferProperties));

    return this;
  }


  public TransferManager addTransfers(List<DataPath> sources, DataPath target) {
    sources.forEach(s -> this.addTransfer(s, target));
    return this;
  }

  public TransferProperties getTransferProperties() {
    return this.transferProperties;
  }

  public TransferManager addTransfer(TransferSourceTarget transferSourceTarget) {
    transfers.put(transferSourceTarget.getSourceDataPath(), transferSourceTarget);
    return this;
  }

  public TransferManager addTargetOperation(TransferResourceOperations transferResourceOperations) {
    transferProperties.addTargetOperations(transferResourceOperations);
    return this;
  }

  public int getExitStatus() {
    if (!this.hasRun) {
      throw new IllegalStateException("To get the exit status, you need to run it first");
    }
    return transferListeners.stream().mapToInt(TransferListener::getExitStatus).sum();
  }

  public String getError() {
    if (!this.hasRun) {
      throw new IllegalStateException("To get an error, you need to run it first");
    }
    return String.join(", ",
      transferListeners.stream()
        .map(TransferListener::getErrorMessages)
        .reduce(Collections.emptyList(), (a, b) -> {
          b.addAll(a);
          return b;
        }));

  }


}





