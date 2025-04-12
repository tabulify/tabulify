package com.tabulify.enrich;

import com.tabulify.spi.DataPathAttribute;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.SelectStream;
import com.tabulify.stream.SelectStreamAbs;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;

public class EnrichSelectStream extends SelectStreamAbs implements SelectStream {

  private final SelectStream wrappedSelectStream;

  public EnrichSelectStream(EnrichDataPath dataPath) {
    super(dataPath);

    try {
      this.wrappedSelectStream = dataPath.wrappedDataPath.getSelectStream();
    } catch (SelectException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public EnrichDataPath getDataPath() {
    return (EnrichDataPath) super.getDataPath();
  }

  @Override
  public boolean next() {
    return this.wrappedSelectStream.next();
  }

  @Override
  public void close() {
    this.wrappedSelectStream.close();
  }

  @Override
  public long getRow() {
    return this.wrappedSelectStream.getRow();
  }

  @Override
  public Object getObject(int columnIndex) {
    if (columnIndex <= this.wrappedSelectStream.getDataPath().getRelationDef().getColumnsSize()) {
      return this.wrappedSelectStream.getObject(columnIndex);
    } else {
      DataPathAttribute attribute = this.getDataPath().mapColumnNameToDataPathAttribute.get(this.getDataPath().getRelationDef().getColumnDef(columnIndex).getColumnName());
      try {
        return this.wrappedSelectStream.getDataPath().getVariable(attribute).getValueOrDefault();
      } catch (NoVariableException | NoValueException e) {
        return null;
      }

    }
  }


  @Override
  public void beforeFirst() {
    this.wrappedSelectStream.beforeFirst();
  }
}
