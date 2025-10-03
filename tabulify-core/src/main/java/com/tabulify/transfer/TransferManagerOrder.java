package com.tabulify.transfer;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.engine.ForeignKeyDag;
import com.tabulify.memory.MemoryDataPathType;
import com.tabulify.memory.queue.MemoryQueueDataPath;
import com.tabulify.model.ColumnDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataSystem;
import com.tabulify.spi.SelectException;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.type.Casts;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tabulify.transfer.TransferManager.LOGGER;

/**
 * An order
 * We created it so that we can test the pre-run operations
 * such as grouping of transfer list
 */
public class TransferManagerOrder {


  private final TransferManager transferManager;
  private final List<TransferSourceTarget> transfersList;
  private final TransferManager.TransferManagerBuilder transferManagerBuilder;

  /**
   * Do we have 2 transfer on the same target
   * Concat Transfer Operation
   */
  private boolean hasConcat;

  /**
   * This list of list should have
   * * all path with stream dependency in one element
   * * all other path in other element one by one
   */
  private List<List<TransferSourceTarget>> transfersGroupedByStreamDependency;


  public TransferManagerOrder(TransferManager transferManager, List<TransferSourceTarget> transfersList) {
    this.transferManager = transferManager;
    this.transferManagerBuilder = transferManager.getMeta();
    this.transfersList = transfersList;
    this.build();
  }

  private void build() {

    /**
     * The transfer is driven by the source
     * You want to move/copy a data set from a source to a target
     * Why driven by source? Simple Dependency
     * * Load dependencies first (You could move a whole schema if you just would select to load the fact table with its dependencies)
     * * Runtime dependency - the runtime dependency is on the source
     */
    List<DataPath> sourceDataPaths = this.transfersList.stream()
      .map(TransferSourceTarget::getSourceDataPath)
      .distinct()
      .collect(Collectors.toList());


    /**
     * Check
     * We can't have multiple source in multiple transfer.
     * It's not supported. You can't select the same source twice.
     * It can happen when an upstream operation generate a path to hold a record.
     * They need to make the name of the data path unique with the record id for instance
     */
    Map<DataPath, Long> bookCountByAuthor = this.transfersList.stream()
      .collect(Collectors.groupingBy(
        TransferSourceTarget::getSourceDataPath,
        Collectors.counting()
      ))
      .entrySet()
      .stream().filter(e -> e.getValue() > 1)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    if (!bookCountByAuthor.isEmpty()) {
      List<TransferSourceTarget> transfersWithSameSource = new ArrayList<>();
      for (DataPath source : bookCountByAuthor.keySet()) {
        for (TransferSourceTarget transferSourceTarget : this.transfersList) {
          if (transferSourceTarget.getSourceDataPath().equals(source)) {
            transfersWithSameSource.add(transferSourceTarget);
          }
        }
      }
      throw new InternalException("Multiple transfer operations with the same source are not supported. An upstream operation has send the same source more than once. Correct the operation. List of incriminated transfer:\n" + transfersWithSameSource.stream().map(TransferSourceTarget::toString).collect(Collectors.joining(",\n")));
    }

    /**
     * Do we have 2 transfer on the same target
     * Concat Transfer Operation
     */
    this.hasConcat = this.buildHasConcat();


    List<DataPath> sourcesInLoadingOrder = ForeignKeyDag
      .createFromPaths(sourceDataPaths)
      .setWithDependency(transferManagerBuilder.withDependencies)
      .getCreateOrdered();

    /**
     * Transfer building is driven by source
     */
    Map<DataPath, TransferSourceTarget> sourceTransferMap = this.buildFinalSourceTransferMap(sourcesInLoadingOrder, transfersList);

    /**
     * Get a list of dependent transfers grouped
     * A dependent transfer is when a data resource has a select stream dependency
     */
    this.transfersGroupedByStreamDependency = this.buildTransfersListGroupedByStreamDependency(sourcesInLoadingOrder, sourceTransferMap);

  }

  /**
   * Detect if there is a concat operation
   */
  private boolean buildHasConcat() {
    if (transfersList.size() == 1) {
      return false;
    }
    Map<DataPath, Long> targetTransferCount = transfersList.stream()
      .collect(Collectors.groupingBy(
        TransferSourceTarget::getTargetDataPath,
        Collectors.counting()
      ));
    switch (targetTransferCount.size()) {
      case 1:
        return true;
      default:
        //noinspection RedundantIfStatement
        if (targetTransferCount.size() != transfersList.size()) {
          /**
           * Mixed concat and non concat operations
           */
          return true;
        }
        return false;
    }
  }


  private Map<DataPath, TransferSourceTarget> buildFinalSourceTransferMap(List<DataPath> sourcesInLoadingOrder, List<TransferSourceTarget> transfersList) {

    /**
     * We build the source/transfer map in 2 steps.
     * * Build the logicalNameTargetMap
     * * Be sure to have the same java object for the same logical target
     */
    Map<String, DataPath> logicalNameTargetMap = transfersList
      .stream()
      .collect(Collectors.toMap(
        e -> e.getTargetDataPath().getLogicalName(),
        TransferSourceTarget::getTargetDataPath,
        (duplicate1, duplicate2) -> duplicate1)
      );
    Map<DataPath, TransferSourceTarget> sourceTransferMap = transfersList
      .stream()
      .map(transferSourceTarget -> {
        String targetLogicalName = transferSourceTarget.getTargetDataPath().getLogicalName();
        DataPath targetDataPath = logicalNameTargetMap.get(targetLogicalName);
        return TransferSourceTarget.create(transferSourceTarget.getSourceDataPath(), targetDataPath);
      })
      .collect(Collectors.toMap(
        TransferSourceTarget::getSourceDataPath,
        tst -> tst
      ));


    /**
     * Adding transfer for dependencies if any
     * <p>
     * If dependencies were added in the step before,
     * we miss some transfers, we add them below
     */
    if (!transferManagerBuilder.withDependencies) {
      return sourceTransferMap;
    }


    /**
     * Calculate the target from the target defined in the first transfer
     * If the target is a document, the calculated target is a sibling
     * If the target is a container, this is the target
     */
    DataPath targetFromFirstTransfer = sourceTransferMap.values().iterator().next().getTargetDataPath();
    for (DataPath sourceDataPath : sourcesInLoadingOrder) {
      TransferSourceTarget transferSourceTarget = sourceTransferMap.get(sourceDataPath);
      if (transferSourceTarget == null) {
        DataPath target;
        if (Tabulars.isDocument(targetFromFirstTransfer)) {
          target = targetFromFirstTransfer.getSibling(sourceDataPath.getName());
        } else {
          target = targetFromFirstTransfer;
        }
        sourceTransferMap.put(sourceDataPath, TransferSourceTarget.create(sourceDataPath, target));
      }
    }

    return sourceTransferMap;

  }

  /**
   * Execute it
   */
  public TransferManagerResult execute() {

    List<TransferListener> transferListeners = new ArrayList<>();

    /**
     * Note: we create the target structure (column)
     * only on the first transfer
     * See {@link TransferManager.targetsSeenInOrders}
     */

    /**
     * The run
     */
    for (List<TransferSourceTarget> transferList : transfersGroupedByStreamDependency) {
      /**
       * Stream dependency transfer?
       */
      switch (transferList.size()) {
        /**
         * No stream dependency on target, the most normal case
         */
        case 1:
          transferListeners.addAll(executeNormalTransfer(transferList.get(0)));
          break;
        /**
         * Transfer with Stream dependency
         */
        default:
          transferListeners.addAll(executeStreamDependentTransfer(transferList));
          break;
      }
    }
    return new TransferManagerResult(this.transferManager, transferListeners);

  }

  /**
   * A transfer of a source transfer without stream dependency
   */
  private List<TransferListener> executeNormalTransfer(TransferSourceTarget transferSource) {

    List<TransferListener> transferListeners = new ArrayList<>();
    TransferSourceTargetOrder transferSourceTarget = toTransferOrder(transferSource);

    /**
     * MetaMove or Not
     * ie move done by:
     * * the data system
     *    * file (copy/insert/)
     *    * database (create sql, ...)
     * * or not (row by row)
     * <p>
     * Special CSV Case: when the format has already the header in the content
     * the meta move on file system cannot be done otherwise the headers will get there 2 times
     */
    boolean metaMove = false;
    boolean headerInContent = transferSourceTarget.getSourceDataPath().hasHeaderInContent();
    Boolean targetExist;
    if (transferSourceTarget.getTransferProperties().targetOperations.contains(TransferResourceOperations.DROP)) {
      targetExist = false;
    } else {
      targetExist = Tabulars.exists(transferSourceTarget.getTargetDataPath());
    }
    boolean sameDataStore = sameDataStore(transferSourceTarget);
    if (sameDataStore && !targetExist) {
      metaMove = true;
    }
    if (sameDataStore && targetExist && !headerInContent) {
      metaMove = true;
    }

    if (metaMove) {

      transferListeners.addAll(executeNormalTransferSameConnection(transferSourceTarget));

    } else {

      transferListeners.addAll(executeNormalTransferCrossConnection(transferSourceTarget));

    }

    /*
     * Do we need to drop the source
     */
    if (
      transferSourceTarget.getTransferProperties().getSourceOperations().contains(TransferResourceOperations.DROP)
    ) {
      if (Tabulars.exists(transferSourceTarget.getSourceDataPath())) {
        Tabulars.drop(transferSourceTarget.getSourceDataPath());
      }
    }

    return transferListeners;
  }

  /**
   * Execute a transfer on the same connection
   * * file to file on the same server
   * * table to table on the same database
   */
  private Collection<? extends TransferListener> executeNormalTransferSameConnection(TransferSourceTargetOrder transferSourceTarget) {

    Connection connection = transferSourceTarget.getSourceDataPath().getConnection();
    final DataSystem sourceDataSystem = connection.getDataSystem();
    try {

      return List.of(sourceDataSystem.transfer(transferSourceTarget));

    } catch (UnsupportedOperationException e) {

      /*
       * The operation may be not implemented
       *
       * Example: {@link TransferOperation#UPSERT} may be only implemented at the record level
       * and not at a set level
       */

      if (Tabulars.exists(transferSourceTarget.getTargetDataPath())) {
        // example: case of a move (ie copy + delete)
        // the copy is successful but the `delete` is not
        throw new UnsupportedOperationException("The target file was created but there was an unsupported operation. Error: " + e.getMessage(), e);
      } else {
        String msg = "The system (" + sourceDataSystem + ") of the data store (" + connection + ") does not support the (" + transferSourceTarget.getTransferProperties().getOperation() + ") transfer operation locally, we are performing it with cross data transfer. Error: " + e.getMessage();
        LOGGER.warning(msg);
        return executeNormalTransferCrossConnection(transferSourceTarget);
      }
    }
  }

  /**
   * Get a list of dependent transfers grouped
   * <p>
   * This step process all transfers: ie
   * * add the data dependencies if any {@link TransferManager.TransferManagerBuilder#withDependency(boolean)} - ie foreign
   * * add the runtime dependencies on select stream dependency if any {@link DataPath#getSelectStreamDependency()}
   * <p>
   * This step is run on start at a {@link #execute()}
   *
   * @param sourcesInLoadingOrder - the order
   * @param transfers             - the transfer by source
   * @return the source transfer to execute grouped by stream dependency
   */
  private List<List<TransferSourceTarget>> buildTransfersListGroupedByStreamDependency(List<DataPath> sourcesInLoadingOrder, Map<DataPath, TransferSourceTarget> transfers) {


    /**
     * Group all data path with stream dependency together
     */
    List<List<DataPath>> sourcesListList = new ArrayList<>();
    for (DataPath dataPath : sourcesInLoadingOrder) {

      boolean hasStreamDependency;
      try {
        DataPath dataPathStreamDependency = dataPath.getSelectStreamDependency();
        hasStreamDependency = dataPathStreamDependency != null;
      } catch (NotFoundException e) {
        hasStreamDependency = false;
      }

      if (hasStreamDependency) {
        /**
         * Do we have already the source in the grouped source
         */
        List<DataPath> sourcesList = sourcesListList
          .stream()
          .filter(l -> l.contains(dataPath))
          .findFirst()
          .orElse(null);

        /**
         * If the source is not in grouped path create it
         * otherwise add it
         */
        if (sourcesList == null) {
          sourcesListList.add(Collections.singletonList(dataPath));
        } else {
          sourcesList.add(dataPath);
        }

      } else {

        sourcesListList.add(Collections.singletonList(dataPath));

      }

    }

    /**
     * Finally transform the grouped source data path into grouped transfer source
     * and return
     */
    List<List<TransferSourceTarget>> transferGrouped = new ArrayList<>();
    for (List<DataPath> sourceDataPathList : sourcesListList) {
      List<TransferSourceTarget> transferSourceTargetList = new ArrayList<>();
      transferGrouped.add(transferSourceTargetList);
      for (DataPath sourceDataPath : sourceDataPathList) {
        transferSourceTargetList.add(transfers.get(sourceDataPath));
      }
    }
    return transferGrouped;

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
   * If a source has no {@link DataPath#getSelectStreamDependency()}, the {@link #executeNormalTransferCrossConnection(TransferSourceTargetOrder)} is normally used
   *
   * @param transferSourceTargets - the source target transfer
   * @return the listener of the transfer
   */
  private List<TransferListener> executeStreamDependentTransfer(List<TransferSourceTarget> transferSourceTargets) {


    List<TransferListener> transferListeners = new ArrayList<>();

    List<List<Object>> streamTransfers = new ArrayList<>();
    for (TransferSourceTarget transferSourceTargetBuilder : transferSourceTargets) {

      TransferSourceTargetOrder transferSourceTarget = toTransferOrder(transferSourceTargetBuilder);
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
        !this.transferManager.targetsSeenInOrders.contains(transferSourceTarget.getTargetDataPath())
      ) {
        transferSourceTarget.targetPreOperationsAndCheck(transferListenerStream, true);
        this.transferManager.targetsSeenInOrders.add(transferSourceTarget.getTargetDataPath());
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

  private TransferSourceTargetOrder toTransferOrder(TransferSourceTarget transferSourceTarget) {

    /**
     * We build it now so that we don't modify the builder
     * We could also create a clone but yeah
     */
    TransferPropertiesSystem transferPropertiesSystem = transferManagerBuilder.transferPropertiesSystemBuilder.build();

    /**
     * The target pre-operation should run only once
     * by transfer manager instance and by target, this method controls this behaviour
     *
     * @param transferSourceTarget - a transfer source target to control
     */
    if (
      !this.transferManager.targetsSeenInOrders.contains(transferSourceTarget.getTargetDataPath())
    ) {

      transferPropertiesSystem
        .addTargetOperations(TransferResourceOperations.CREATE)
        .setRunPreDataOperation(true);
      this.transferManager.targetsSeenInOrders.add(transferSourceTarget.getTargetDataPath());

    } else {

      transferPropertiesSystem
        .deleteTargetOperations()
        .deleteSourceOperations()
        .setRunPreDataOperation(false);

    }

    return transferSourceTarget.buildOrder(transferPropertiesSystem);

  }

  /**
   * Transfer of a data resource from:
   * * one connection to another
   * * record by record
   * <p>
   * There is also a transfer from multiple source to one target
   * when the source generation is dependent called {@link #executeStreamDependentTransfer(List)}
   * <p>
   * This function supports the loading of data with multiple threads (ie
   * when the {@link TransferPropertiesCross#setTargetWorkerCount(int)} is bigger than one)
   *
   * @param transferSourceTarget - the source target transfer to execute
   * @return the result
   */
  private List<TransferListener> executeNormalTransferCrossConnection(TransferSourceTargetOrder transferSourceTarget) {


    DataPath sourceDataPath = transferSourceTarget.getSourceDataPath();
    DataPath targetDataPath = transferSourceTarget.getTargetDataPath();
    TransferPropertiesSystem transferPropertiesSystem = transferSourceTarget.getTransferProperties();


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
    transferSourceTarget.sourcePreChecks();
    transferSourceTarget.targetPreOperationsAndCheck(transferListenerStream, true);

    /*
     * Single thread ?
     */
    TransferPropertiesCross transferPropertiesCrossManager = this.getTransferProperties();
    int targetWorkerCount = transferPropertiesCrossManager.getTargetWorkerCount();
    if (targetWorkerCount == 1) {
      try (
        SelectStream sourceSelectStream = sourceDataPath.getSelectStreamSafe();
        InsertStream targetInsertStream = targetDataPath.getInsertStream(sourceDataPath, transferPropertiesSystem)
      ) {

        transferListenerStream.setMethod(targetInsertStream.getMethod());
        transferListenerStream.addInsertListener(targetInsertStream.getInsertStreamListener());

        /**
         * We send only the column that are in the mapping in column position order
         */
        List<Integer> sourceColumnInAscOrder = transferSourceTarget.getTransferSourceTargetColumnMapping()
          .keySet()
          .stream()
          .map(ColumnDef::getColumnPosition)
          .sorted()
          .collect(Collectors.toList());
        // Run
        while (sourceSelectStream.next()) {
          List<Object> objects = new ArrayList<>();
          for (Integer sourceColumnPosition : sourceColumnInAscOrder) {
            Object object = sourceSelectStream.getObject(sourceColumnPosition);
            objects.add(object);
          }
          targetInsertStream.insert(objects);
        }

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
      int timeout = transferPropertiesCrossManager.getTimeOut();
      Tabular tabular = sourceDataPath.getConnection().getTabular();
      MemoryQueueDataPath buffer = (MemoryQueueDataPath) ((MemoryQueueDataPath) tabular.getMemoryConnection()
        .getTypedDataPath(MemoryDataPathType.QUEUE, "buffer"))
        .setTimeout(timeout)
        .setCapacity(transferPropertiesCrossManager.getBufferSize())
        .getOrCreateRelationDef()
        .mergeStruct(sourceDataPath.getRelationDef())
        .getDataPath();
      Tabulars.create(buffer);

      try {

        // Start the viewer
        TransferWorkerMetricsViewer transferWorkerMetricsViewer = new TransferWorkerMetricsViewer(buffer, transferPropertiesCrossManager, producerWorkIsDone, consumerWorkIsDone);
        Thread viewer = new Thread(transferWorkerMetricsViewer);
        viewer.start();

        // Start the producer thread
        TransferWorkerProducer transferWorkerProducer = new TransferWorkerProducer(
          TransferSourceTarget.create(sourceDataPath, buffer).buildOrder(transferPropertiesSystem),
          transferWorkerMetricsViewer
        );
        Thread producer = new Thread(transferWorkerProducer);
        producer.start();

        // Start the consumer / target threads
        ExecutorService targetWorkExecutor = Executors.newFixedThreadPool(targetWorkerCount);
        for (int i = 0; i < targetWorkerCount; i++) {

          targetWorkExecutor.execute(
            new TransferWorkerConsumer(
              TransferSourceTarget.create(buffer, targetDataPath).buildOrder(transferPropertiesSystem),
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
        return Casts.castToNewListSafe(
          transferWorkerMetricsViewer.getListenersStream(),
          TransferListener.class
        );

      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }


    }


  }

  private boolean sameDataStore(TransferSourceTargetOrder transferSourceTarget) {
    return transferSourceTarget.getSourceDataPath().getConnection().getServiceId().equals(transferSourceTarget.getTargetDataPath().getConnection().getServiceId());
  }

  private TransferPropertiesCross getTransferProperties() {
    return transferManagerBuilder.transferPropertiesCross;
  }

  /**
   * A list of transfers that should be executed together
   */
  public List<List<TransferSourceTarget>> getGroupedTransferList() {
    return this.transfersGroupedByStreamDependency;
  }

}
