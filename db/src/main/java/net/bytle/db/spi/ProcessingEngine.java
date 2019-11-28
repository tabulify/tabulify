package net.bytle.db.spi;

import net.bytle.db.model.ColumnDef;

public abstract class ProcessingEngine {

    public abstract <T> T getMax(ColumnDef<T> columnDef);

    public abstract <T> T getMin(ColumnDef<T> columnDef);

    public abstract DataPath getQuery(String query);

}
