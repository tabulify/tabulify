package net.bytle.db.enrich;

import net.bytle.db.spi.DataPathAttribute;
import net.bytle.db.spi.SelectException;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.SelectStreamAbs;
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
