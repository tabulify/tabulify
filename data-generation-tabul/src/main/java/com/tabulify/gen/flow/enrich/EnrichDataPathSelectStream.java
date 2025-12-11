package com.tabulify.gen.flow.enrich;

import com.tabulify.gen.GenColumnDef;
import com.tabulify.gen.generator.CollectionGenerator;
import com.tabulify.gen.generator.DataPathStreamGenerator;
import com.tabulify.gen.generator.MetaAttributeGenerator;
import com.tabulify.model.ColumnDef;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.SelectStream;
import com.tabulify.stream.SelectStreamAbs;
import com.tabulify.exception.InternalException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnrichDataPathSelectStream extends SelectStreamAbs implements SelectStream {

  private final SelectStream wrappedSelectStream;
  private final List<CollectionGenerator<?>> collectionGenerators;
  private Map<ColumnDef<?>, Object> row = new HashMap<>();

  public EnrichDataPathSelectStream(EnrichDataPath dataPath) {
    super(dataPath);

    try {
      this.wrappedSelectStream = dataPath.wrappedDataPath.getSelectStream();
    } catch (SelectException e) {
      throw new RuntimeException(e);
    }

    /**
     * Build the generators
     * (Hack)
     */
    collectionGenerators = dataPath.getRelationDef().buildGeneratorInCreateOrder();
    /**
     * Inject the stream
     */
    for (GenColumnDef columnDef : dataPath.getRelationDef().getColumnDefs()) {
      CollectionGenerator<?> generator = columnDef.getGenerator();
      if (generator == null) {
        throw new InternalException("The generator of the column (" + columnDef + ") is empty");
      }
      if (generator instanceof DataPathStreamGenerator) {
        DataPathStreamGenerator<?> streamGenerator = (DataPathStreamGenerator<?>) generator;
        streamGenerator.setSelectStream(this.wrappedSelectStream);
      }
      if (generator instanceof MetaAttributeGenerator) {
        MetaAttributeGenerator<?> metaGenerator = (MetaAttributeGenerator<?>) generator;
        metaGenerator.setMeta(dataPath.wrappedDataPath);
      }
    }
  }

  @Override
  public EnrichDataPath getDataPath() {
    return (EnrichDataPath) super.getDataPath();
  }

  @Override
  public boolean next() {
    boolean next = this.wrappedSelectStream.next();
    if (!next) {
      return next;
    }
    this.row = this.getDataPath().getRelationDef().buildRowFromGenerators(collectionGenerators);
    return next;
  }

  @Override
  public void close() {
    this.wrappedSelectStream.close();
  }

  @Override
  public boolean isClosed() {
    return this.wrappedSelectStream.isClosed();
  }

  @Override
  public long getRecordId() {
    return this.wrappedSelectStream.getRecordId();
  }


  @Override
  public Object getObject(ColumnDef<?> columnDef) {
    if (!(columnDef instanceof GenColumnDef)) {
      throw new InternalException("The column def should be a generator column. It's a " + columnDef.getClass().getSimpleName());
    }
    GenColumnDef<?> genColumnDef = (GenColumnDef<?>) columnDef;
    Object actualValue = this.row.get(genColumnDef);
    if (actualValue != null) {
      return actualValue;
    }
    Object value = genColumnDef
      .getGenerator()
      .getNewValue();
    this.row.put(genColumnDef, value);
    return value;
  }


  @Override
  public void beforeFirst() {
    this.wrappedSelectStream.beforeFirst();
  }
}
