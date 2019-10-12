package net.bytle.db.spi;

public abstract class SelectStreamAbs {

    private final DataPath dataPath;

    public SelectStreamAbs(DataPath dataPath) {
        this.dataPath = dataPath;
    }


    public DataPath getDataPath() {
        return dataPath;
    }


}
