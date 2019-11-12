package net.bytle.db.transfer;

import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStreamListener;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Created by gerard on 29-01-2016.
 */
public class TransferMetricsViewer implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(TransferMetricsViewer.class.getPackage().toString()+Thread.currentThread().getName());


    private final DataPath queue;
    private final AtomicBoolean producerWorkIsDone;
    private final Integer queueSize;
    private final List<InsertStreamListener> insertStreamListeners;
    private final DataPath metricsFilePath;
    private final AtomicBoolean consumerWorkIsDone;



    public TransferMetricsViewer(

            DataPath queue,
            TransferProperties transferProperties,
            List<InsertStreamListener> insertStreamListeners,
            AtomicBoolean producerWorkIsDone,
            AtomicBoolean consumerWorkIsDone) {

        this.queue = queue;
        this.producerWorkIsDone = producerWorkIsDone;
        this.queueSize = transferProperties.getQueueSize();
        this.insertStreamListeners = insertStreamListeners;
        this.metricsFilePath = transferProperties.getMetricsPath();
        this.consumerWorkIsDone = consumerWorkIsDone;


    }

    @Override
    public void run() {

        try {

            LOGGER.fine("Viewer: " + Thread.currentThread().getName() + ": Started");

            Writer writer;
            if (metricsFilePath !=null) {
                writer = new FileWriter(Paths.get(metricsFilePath.getPath()).toFile());
            } else {
                writer = new OutputStreamWriter(System.out);
            }
            BufferedWriter  outputStream = new BufferedWriter(writer);


            int n = 0;
            while (!producerWorkIsDone.get() & !consumerWorkIsDone.get()) {

                // 5 seconds after the first one
                // To be able to of the data if
                // the load is going below the 5 seconds
                if (n > 0) {
                    Thread.sleep(5000);
                };
                n++;

                String timeMillis = getCurrentTimeStamp();

                int size = Tabulars.getSize(queue);
                String bufferSizeCsv = timeMillis + ", Buffer Size, " + size;
                outputStream.write(bufferSizeCsv);
                outputStream.newLine();

                String bufferMaxSizeCsv = timeMillis + ", Buffer MaxSize, " + this.queueSize;
                outputStream.write(bufferMaxSizeCsv);
                outputStream.newLine();

                Double ratio = Double.valueOf(size) / this.queueSize * 100;
                String bufferRatioCsv = timeMillis + ", Buffer Ratio, " + ratio;
                outputStream.write(bufferRatioCsv);
                outputStream.newLine();

                LOGGER.fine("Viewer: " + Thread.currentThread().getName() + ": The buffer (between producer and consumer) is "+ratio+"% full (Size:"+size+", MaxSize:"+this.queueSize +")");


                for (InsertStreamListener insertStreamListener : insertStreamListeners) {

                    // Commits
                    Integer commits = insertStreamListener.getCommits();
                    final String inputStreamName = insertStreamListener.getInsertStream().getName();

                    String commitsCsv = timeMillis + ", " + commits + " Commits, " + inputStreamName;
                    System.out.println(commitsCsv);
                    outputStream.write(commitsCsv);
                    outputStream.newLine();

                    // Records
                    String recordsCsv = timeMillis + ", " + insertStreamListener.getRowCount() + " Records, " + inputStreamName;
                    System.out.println(recordsCsv);
                    outputStream.write(recordsCsv);
                    outputStream.newLine();

                }


                // Flush
                outputStream.flush();

            }

            // We don't want to close the console standard output
            // Otherwise all subsequent calls are lost in the universe
            if (metricsFilePath !=null) {
                outputStream.close();
                writer.close();
            }
            LOGGER.fine("End of the viewer");

        } catch (InterruptedException | IOException e) {

            throw new RuntimeException(e);
        }

    }

    public String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }


}
