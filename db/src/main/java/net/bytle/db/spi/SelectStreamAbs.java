package net.bytle.db.spi;

import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.SelectStreamListener;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class SelectStreamAbs implements SelectStream {


    private final DataPath dataPath;
    protected SelectStreamListener selectStreamListener = SelectStreamListener.of(this);
    private String name;

    public SelectStreamAbs(DataPath dataPath) {

        this.dataPath = dataPath;

    }

    /**
     * Retrieves and removes the head of this data path, or returns null if this queue is empty.
     *
     * @param timeout
     * @param timeUnit
     * @return
     */
    public abstract boolean next(Integer timeout, TimeUnit timeUnit);

    @Override
    public SelectStreamListener getSelectStreamListener() {
        return this.selectStreamListener;
    }

    @Override
    public SelectStream setName(String name) {
        this.name = name;
        return this;
    }


    @Override
    public List<Object> getObjects() {
        return IntStream.of(this.dataPath.getDataDef().getColumnDefs().size())
                .mapToObj(this::getObject)
                .collect(Collectors.toList());
    }


}
