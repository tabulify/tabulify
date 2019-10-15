package net.bytle.db.spi;

import net.bytle.db.database.Database;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.DataType;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;

public abstract class TableSystem implements AutoCloseable {

    public abstract DataPath getDataPath(DataUri dataUri);

    public abstract DataPath getDataPath(String... name);

    public abstract Boolean exists(DataPath dataPath);

    public abstract SelectStream getSelectStream(DataPath dataPath);

    public abstract Database getDatabase();

    public abstract <T> T getMax(ColumnDef<T> columnDef);

    public abstract boolean isContainer(DataPath dataPath);

    public abstract DataPath create(DataPath dataPath);

    // The product name (for a jdbc database: sql server, oracle, hive ...
    public abstract String getProductName();

    public abstract DataType getDataType(Integer typeCode);

    public abstract void drop(DataPath dataPath);

    public abstract void delete(DataPath dataPath);

    public abstract void truncate(DataPath dataPath);

    public abstract <T> T getMin(ColumnDef<T> columnDef);

    public abstract void dropForeignKey(ForeignKeyDef foreignKeyDef);

    public abstract SelectStream getSelectStream(String query);

    public abstract TableSystemProvider getProvider();

    public abstract InsertStream getInsertStream(DataPath dataPath);

}
