package com.tabulify.spi;

import com.tabulify.model.ColumnDef;

public abstract class ProcessingEngine {

    public abstract Object getMax(ColumnDef columnDef);

    public abstract Object getMin(ColumnDef columnDef);


}
