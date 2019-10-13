package net.bytle.db.spi;

import net.bytle.db.database.Database;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;

public abstract class TableSystem implements AutoCloseable {

    public abstract DataPath getDataPath(DataUri dataUri);

    public abstract DataPath getDataPath(DataPath dataPath, String... name);

    public abstract Boolean exists(DataPath dataPath);

    public abstract SelectStream getSelectStream(DataPath dataPath);

    public abstract Database getDatabase();

    public abstract <T> T getMax(ColumnDef<T> columnDef);

    public abstract boolean isContainer(DataPath dataPath);

}
