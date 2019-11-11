package net.bytle.db.move;

import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Created by gerard on 29-01-2016.
 */
public class ResultSetLoaderMetricsViewer implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ResultSetLoaderMetricsViewer.class.getPackage().toString()+Thread.currentThread().getName());


    private final DataPath queue;
    private final AtomicBoolean producerWorkIsDone;
    private final Integer maxBufferSize;
    private final List<MoveListener> insertStreamListeners;
    private final String metricsFilePath;
    private final AtomicBoolean consumerWorkIsDone;



    public ResultSetLoaderMetricsViewer(

            DataPath queue,
            Integer maxBufferSize,
            List<MoveListener> insertStreamListeners,
            String metricsFilePath,
            AtomicBoolean producerWorkIsDone,
            AtomicBoolean consumerWorkIsDone) {

        this.queue = queue;
        this.producerWorkIsDone = producerWorkIsDone;
        this.maxBufferSize = maxBufferSize;
        this.insertStreamListeners = insertStreamListeners;
        this.metricsFilePath = metricsFilePath;
        this.consumerWorkIsDone = consumerWorkIsDone;


    }

    @Override
    public void run() {

        try {

            LOGGER.fine("Viewer: " + Thread.currentThread().getName() + ": Started");

            Writer writer;
            if (metricsFilePath !=null) {
                writer = new FileWriter(metricsFilePath);
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

                String bufferMaxSizeCsv = timeMillis + ", Buffer MaxSize, " + this.maxBufferSize;
                outputStream.write(bufferMaxSizeCsv);
                outputStream.newLine();

                Double ratio = Double.valueOf(size) / this.maxBufferSize * 100;
                String bufferRatioCsv = timeMillis + ", Buffer Ratio, " + ratio;
                outputStream.write(bufferRatioCsv);
                outputStream.newLine();

                LOGGER.fine("Viewer: " + Thread.currentThread().getName() + ": The buffer (between producer and consumer) is "+ratio+"% full (Size:"+size+", MaxSize:"+this.maxBufferSize+")");


                for (MoveListener insertStreamListener : insertStreamListeners) {

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

        } catch (InterruptedException e) {

            throw new RuntimeException(e);
        } catch (IOException e) {

            throw new RuntimeException(e);
        }

    }

    public String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }


}
