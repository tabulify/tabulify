package net.bytle.db.spi;

import net.bytle.db.stream.SelectStream;

import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class SelectStreamAbs implements SelectStream {



    /**
     * Retrieves and removes the head of this data path, or returns null if this queue is empty.
     * @param i
     * @param timeUnit
     * @return
     */
    public abstract List<Object> poll(int i, TimeUnit timeUnit);



}
