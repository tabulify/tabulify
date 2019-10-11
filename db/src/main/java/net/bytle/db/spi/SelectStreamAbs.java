package net.bytle.db.spi;

public abstract class SelectStreamAbs {


    // TODO: Hack to not break the code everywhere
    // delete after data path implementation
    public DataPath getDataPath() {
        return null;
    }


}
