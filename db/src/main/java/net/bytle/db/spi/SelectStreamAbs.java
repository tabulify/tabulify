package net.bytle.db.spi;

import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.SelectStreamListener;

import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class SelectStreamAbs implements SelectStream {


    private final DataPath dataPath;
    protected SelectStreamListener selectStreamListener = SelectStreamListener.of(this);

    public SelectStreamAbs(DataPath dataPath) {

        this.dataPath = dataPath;

    }

    /**
     * Retrieves and removes the head of this data path, or returns null if this queue is empty.
     * @param timeout
     * @param timeUnit
     * @return
     */
    public abstract boolean next(Integer timeout, TimeUnit timeUnit);

    @Override
    public SelectStreamListener getSelectStreamListener() {
        return this.selectStreamListener;
    }



}
