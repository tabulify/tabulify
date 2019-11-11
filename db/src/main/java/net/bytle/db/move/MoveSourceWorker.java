package net.bytle.db.move;


import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A worker that takes data from the source and insert them into the memory queue
 */
public class MoveSourceWorker implements Runnable {


    private final DataPath sourceDataPath;
    private final DataPath queue;
    private final Integer feedbackFrequency;
    private final List<InsertStreamListener> listeners;


    /**
     *  @param sourceDataPath
     * @param targetDataPath (A blocking queue !)
     * @param listeners The listener
     */
    public MoveSourceWorker(DataPath sourceDataPath, DataPath targetDataPath, List<InsertStreamListener> listeners, MoveProperties moveProperties) {

        this.sourceDataPath = sourceDataPath;
        this.queue = targetDataPath;
        this.listeners = listeners;
        this.feedbackFrequency = moveProperties.getFeedbackFrequency() ;

    }


    @Override
    public void run() {

        InsertStream insertStream = Tabulars.getInsertStream(queue)
                .setName("Producer: " + Thread.currentThread().getName())
                .setFeedbackFrequency(feedbackFrequency);
        InsertStreamListener listener = insertStream.getInsertStreamListener();
        listeners.add(listener);
        try {


            SelectStream selectStream = Tabulars.getSelectStream(sourceDataPath);

            List<Object> objects;
            int columnCount = sourceDataPath.getDataDef().getColumnDefs().size();
            while (selectStream.next()) {

                objects = IntStream.of(columnCount)
                        .mapToObj(selectStream::getObject)
                        .collect(Collectors.toList());
                insertStream.insert(objects);
                //  queue.offer(objects, timeout, TimeUnit.SECONDS);

            }

            insertStream.close();


        } catch (Exception e) {
            // TODO - bad he ...
            listener.addException(e);
            throw new RuntimeException(e);

        }

    }
}

