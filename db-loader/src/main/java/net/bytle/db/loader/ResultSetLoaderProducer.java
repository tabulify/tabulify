package net.bytle.db.loader;


import net.bytle.db.model.RelationDef;
import net.bytle.db.stream.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by gerard on 29-01-2016.
 */
public class ResultSetLoaderProducer implements Runnable {


    private final RelationDef sourceDef;
    private final BlockingQueue<List<Object>> queue;
    private final Integer feedbackFrequency;
    private final List<InsertStreamListener> listeners;



    /**
     *  @param sourceDef
     * @param q
     * @param listeners The listener
     */
    public ResultSetLoaderProducer(RelationDef sourceDef, BlockingQueue<List<Object>> q, List<InsertStreamListener> listeners, Integer feedbackFrequency) {

        this.sourceDef = sourceDef;
        this.queue = q;
        this.listeners = listeners;
        this.feedbackFrequency = feedbackFrequency;

    }


    @Override
    public void run() {

        String producerName = "Producer: " + Thread.currentThread().getName();
        InsertStream insertStream = QueueInsertStream.get(queue)
                .setName(producerName)
                .setFeedbackFrequency(feedbackFrequency);
        InsertStreamListener listener = insertStream.getInsertStreamListener();
        listeners.add(listener);
        try {


            SelectStream selectStream = Streams.getSelectStream(sourceDef);

            List<Object> objects;
            int columnCount = sourceDef.getColumnDefs().size();
            while (selectStream.next()) {


                objects = new ArrayList<>(columnCount);
                for (int i = 0; i < columnCount; i++) {

                    //System.out.println("Adding column "+ i+ ": " + sourceMetaData.getColumnName(i));
                    objects.add(selectStream.getObject(i));

                }
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

