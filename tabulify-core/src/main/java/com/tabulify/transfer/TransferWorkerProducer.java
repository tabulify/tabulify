package com.tabulify.transfer;


import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.InsertStreamListener;
import com.tabulify.stream.SelectStream;

/**
 * A worker that takes data from the source and insert them into the memory queue
 */
public class TransferWorkerProducer implements Runnable {


  private final DataPath sourceDataPath;
  private final DataPath buffer;
  private final TransferListenerStream transferListenerStream;
  private final TransferProperties transferProperties;



  public TransferWorkerProducer(TransferSourceTarget transferSourceTarget, TransferWorkerMetricsViewer transferWorkerMetricsViewer) {

    this.sourceDataPath = transferSourceTarget.getSourceDataPath();
    this.buffer = transferSourceTarget.getTargetDataPath();
    this.transferProperties = transferSourceTarget.getTransferProperties();

    this.transferListenerStream =  new TransferListenerStream(transferSourceTarget);
    transferListenerStream.setType(TransferType.THREAD_CROSS);
    transferListenerStream.startTimer();
    transferWorkerMetricsViewer.addStreamListener(transferListenerStream);
  }


  @Override
  public void run() {

    String threadName = Thread.currentThread().getName();
    String streamName = "Producer: " + threadName;
    try (

      SelectStream selectStream = sourceDataPath.getSelectStream()
        .setName(streamName);

      InsertStream insertStream = buffer.getInsertStream(sourceDataPath, transferProperties)
        .setName(streamName);

    ) {

      // The feedback
      InsertStreamListener insertStreamListener = insertStream.getInsertStreamListener();
      transferListenerStream.addInsertListener(insertStreamListener);

      // The transfer
      while (selectStream.next()) {

        insertStream.insert(selectStream.getObjects());

      }

    } catch (Exception e) {

      transferListenerStream.addException(e);
      throw new RuntimeException(e);

    }

    transferListenerStream.stopTimer();
    TransferLog.LOGGER.info("The producer "+threadName+" has finished.");

  }
}

