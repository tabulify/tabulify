package com.tabulify.transfer;

import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class TransferWorkerConsumer implements Runnable {


  private final DataPath buffer;
  private final AtomicBoolean producerWorkIsDone;
  private final TransferSourceTarget transferSourceTarget;
  private final TransferListenerStream transferListenerStream;
  private final TransferProperties transferProperties;
  private final DataPath target;


  public TransferWorkerConsumer(
    TransferSourceTarget transferSourceTarget,
    AtomicBoolean producerWorkIsDone,
    TransferWorkerMetricsViewer transferWorkerMetricsViewer
  ) {
    this.buffer = transferSourceTarget.getSourceDataPath();
    this.target = transferSourceTarget.getTargetDataPath();
    this.transferProperties = transferSourceTarget.getTransferProperties();
    this.transferSourceTarget = transferSourceTarget;
    this.producerWorkIsDone = producerWorkIsDone;
    this.transferListenerStream = new TransferListenerStream(transferSourceTarget);
    transferListenerStream.setType(TransferType.THREAD_CROSS);
    transferListenerStream.startTimer();
    transferWorkerMetricsViewer.addStreamListener(transferListenerStream);

  }

  @Override
  public void run() {
    String threadName = Thread.currentThread().getName();
    String name = "Consumer: " + threadName;
    try (
      SelectStream selectStream = buffer.getSelectStream().setName(name);
      InsertStream insertStream = target.getInsertStream(buffer, transferProperties).setName(name);
    ) {

      transferListenerStream.addInsertListener(insertStream.getInsertStreamListener());

      List<?> objects;
      while (true) {
        while (true) {
          Integer timeOut = 1;
          if (!selectStream.next(timeOut, TimeUnit.SECONDS)) break;

          objects = selectStream.getObjects();
          insertStream.insert(objects);

        }
        if (producerWorkIsDone.get()) {
          break;
        }
      }

    } catch (Exception e) {
      transferListenerStream.addException(e);
      throw new RuntimeException(e);
    }

    transferListenerStream.stopTimer();
    TransferLog.LOGGER.info("The Consumer " + threadName + " has finished.");
  }
}
