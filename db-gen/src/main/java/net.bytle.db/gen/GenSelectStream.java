package net.bytle.db.gen;

import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.SelectStreamAbs;

import java.sql.Clob;
import java.util.concurrent.TimeUnit;

public class GenSelectStream extends SelectStreamAbs {



  public GenSelectStream(DataPath dataPath) {

    super(dataPath);
    GenDataPath genDataPath = (GenDataPath) dataPath;
    GenDataDef dataDef = genDataPath.getDataDef();

  }



  @Override
  public boolean next() {
    return false;
  }

  @Override
  public void close() {

  }

  @Override
  public String getString(int columnIndex) {
    return null;
  }

  @Override
  public int getRow() {
    return 0;
  }

  @Override
  public Object getObject(int columnIndex) {
    return null;
  }

  @Override
  public TableDef getSelectDataDef() {
    return null;
  }

  @Override
  public double getDouble(int columnIndex) {
    return 0;
  }

  @Override
  public Clob getClob(int columnIndex) {
    return null;
  }

  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {
    return false;
  }

  @Override
  public Integer getInteger(int columnIndex) {
    return null;
  }

  @Override
  public Object getObject(String columnName) {
    return null;
  }

  @Override
  public void beforeFirst() {

  }

  @Override
  public void execute() {

  }


}
