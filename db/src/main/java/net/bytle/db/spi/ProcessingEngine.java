package net.bytle.db.spi;

import net.bytle.db.model.ColumnDef;

public abstract class ProcessingEngine {

    public abstract Object getMax(ColumnDef columnDef);

    public abstract Object getMin(ColumnDef columnDef);


}
