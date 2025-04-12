package com.tabulify.transfer;

import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.InsertStreamListener;
import net.bytle.type.time.Timestamp;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gerard on 29-01-2016.
 */
public class TransferWorkerMetricsViewer implements Runnable {


  public static final String MAIN_THREAD = "Main";
  public static final String BUFFER_SIZE = "BufferSize";
  public static final String BUFFER_MAX_SIZE = "BufferMaxSize";
  public static final String BUFFER_RATIO = "BufferRatio";
  public static final String COMMITS = "Commits";
  private static final Object RECORDS = "Records";
  public static final int pollingFrequencyMs = 1000;
  private final DataPath buffer;
  private final AtomicBoolean producerWorkIsDone;
  private final Integer bufferSize;
  private final DataPath metricsFilePath;
  private final AtomicBoolean consumerWorkIsDone;
  private List<TransferListenerStream> transferListenerStreams = new ArrayList<>();


  public TransferWorkerMetricsViewer(

    DataPath buffer,
    TransferProperties transferProperties,
    AtomicBoolean producerWorkIsDone,
    AtomicBoolean consumerWorkIsDone) {

    this.buffer = buffer;
    this.producerWorkIsDone = producerWorkIsDone;
    this.bufferSize = transferProperties.getBufferSize();
    this.metricsFilePath = transferProperties.getMetricsPath();
    this.consumerWorkIsDone = consumerWorkIsDone;


  }

  @Override
  public void run() {


    TransferLog.LOGGER.fine("Viewer: " + Thread.currentThread().getName() + ": Started");

    DataPath dataPath = buffer.getConnection().getTabular().getCurrentLocalDirectoryConnection().getDataPath("metrics");
    if (metricsFilePath != null) {
      dataPath = metricsFilePath;
    }
    dataPath.getOrCreateRelationDef()
      .addColumn("run", Types.TIMESTAMP)
      .addColumn("timestamp", Types.TIMESTAMP)
      .addColumn("metric", Types.VARCHAR)
      .addColumn("value", Types.BIGINT)
      .addColumn("worker", Types.VARCHAR);
    if (Tabulars.exists(dataPath)) {
      Tabulars.drop(dataPath);
    }
    Tabulars.create(dataPath);
    java.sql.Timestamp run = Timestamp.createFromNowLocalSystem().toSqlTimestamp();
    try (InsertStream insertStream = dataPath.getInsertStream()) {

      int n = 0;
      boolean isLastLoop = false;
      while (true) {

        if (producerWorkIsDone.get() && consumerWorkIsDone.get()) {
          if (!isLastLoop) {
            /**
             * We give one loop more
             * to be able to grab the last data (ie rows and commits)
             */
            isLastLoop = true;
          } else {
            break;
          }
        }

        // 1 seconds after the first one
        // To be able to of the data if
        // the load is going below the 1 seconds
        if (n > 0) {
          Thread.sleep(pollingFrequencyMs);
        }

        n++;

        java.sql.Timestamp now = Timestamp.createFromNowLocalSystem().toSqlTimestamp();

        long count = buffer.getCount();
        insertStream.insert(run, now, BUFFER_SIZE, count, MAIN_THREAD);
        insertStream.insert(run, now, BUFFER_MAX_SIZE, this.bufferSize, MAIN_THREAD);
        double ratio = (double) count / this.bufferSize * 100;
        insertStream.insert(run, now, BUFFER_RATIO, ratio, MAIN_THREAD);

        TransferLog.LOGGER.fine("Viewer: " + Thread.currentThread().getName() + ": The buffer (between producer and consumer) is " + ratio + "% full (Size:" + count + ", MaxSize:" + this.bufferSize + ")");

        for (TransferListenerStream transferListenerStream : this.transferListenerStreams) {

          InsertStreamListener insertStreamListener = transferListenerStream.getInsertStreamListeners();
          // Commits
          final String inputStreamName = insertStreamListener.getInsertStream().getName();
          Long rowCount = insertStreamListener.getRowCount();
          Integer commits = insertStreamListener.getCommits();
          insertStream.insert(run, now, COMMITS, commits, inputStreamName);
          insertStream.insert(run, now, RECORDS, rowCount, inputStreamName);


        }


      }

      TransferLog.LOGGER.fine("The viewer thread has ended");

    } catch (
      InterruptedException e) {

      throw new RuntimeException(e);
    }

  }


  public TransferWorkerMetricsViewer addStreamListener(TransferListenerStream transferListenerStream) {
    this.transferListenerStreams.add(transferListenerStream);
    return this;
  }

  public List<TransferListenerStream> getListenersStream() {
    return this.transferListenerStreams;
  }
}
