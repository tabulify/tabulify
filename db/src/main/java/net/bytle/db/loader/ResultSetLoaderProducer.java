package net.bytle.db.loader;


import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by gerard on 29-01-2016.
 */
public class ResultSetLoaderProducer implements Runnable {


    private final DataPath sourceDataPath;
    private final DataPath queue;
    private final Integer feedbackFrequency;
    private final List<InsertStreamListener> listeners;



    /**
     *  @param sourceDataPath
     * @param targetDataPath (A blocking queue !)
     * @param listeners The listener
     */
    public ResultSetLoaderProducer(DataPath sourceDataPath, DataPath targetDataPath, List<InsertStreamListener> listeners, Integer feedbackFrequency) {

        this.sourceDataPath = sourceDataPath;
        this.queue = targetDataPath;
        this.listeners = listeners;
        this.feedbackFrequency = feedbackFrequency;

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

            }

            insertStream.close();


        } catch (Exception e) {
            // TODO - bad he ...
            listener.addException(e);
            throw new RuntimeException(e);

        }

    }
}

