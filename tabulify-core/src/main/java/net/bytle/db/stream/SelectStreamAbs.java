package net.bytle.db.stream;

import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;
import net.bytle.exception.CastException;
import net.bytle.exception.NoColumnException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class SelectStreamAbs implements Stream, SelectStream {


  private final DataPath dataPath;
  protected SelectStreamListener selectStreamListener = SelectStreamListener.of(this);
  private String name;


  public SelectStreamAbs(DataPath dataPath) {

    this.dataPath = dataPath;

  }

  /**
   * Retrieves and removes the head of this data path, or returns null if this queue is empty.
   *
   * @param timeout - the numeric timeout to retrieve an object
   * @param timeUnit - the unit
   * @return false if this is the end
   */
  public boolean next(Integer timeout, TimeUnit timeUnit) {
    throw new RuntimeException("Unsupported operations");
  }

  @Override
  public SelectStreamListener getSelectStreamListener() {
    return this.selectStreamListener;
  }

  @Override
  public SelectStream setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public <T> T getObject(String columnName, Class<T> clazz) {
    try {
      return this.getDataPath().getConnection().getObject(getObject(columnName), clazz);
    } catch (CastException e) {
      throw new ClassCastException(e.getMessage());
    }
  }

  @Override
  public <T> T getObject(int index, Class<T> clazz) {
    try {
      return this.getDataPath().getConnection().getObject(getObject(index), clazz);
    } catch (CastException e) {
      throw new ClassCastException(e.getMessage());
    }
  }


  @Override
  public List<?> getObjects() {
    List<Object> objects = new ArrayList<>();
    for (int i = 1; i <= this.dataPath.getOrCreateRelationDef().getColumnsSize(); i++) {
      objects.add(getObject(i));
    }
    return objects;
  }

  @Override
  public DataPath getDataPath() {
    return this.dataPath;
  }

  @Override
  public Date getDate(int columnIndex) {

    return getObject(columnIndex, Date.class);
  }

  @Override
  public Timestamp getTimestamp(int columnIndex) {

    return getObject(columnIndex, Timestamp.class);

  }

  @Override
  public Boolean getBoolean(int columnIndex) {

    return getObject(columnIndex, Boolean.class);

  }

  @Override
  public SQLXML getSqlXml(int columnIndex) {

    return getObject(columnIndex, SQLXML.class);

  }

  @Override
  public Integer getInteger(int columnIndex) {

    return getObject(columnIndex, Integer.class);

  }

  @Override
  public Double getDouble(int columnIndex) {
    return getObject(columnIndex, Double.class);
  }

  @Override
  public Time getTime(int columnIndex) {

    return getObject(columnIndex, Time.class);
  }

  @Override
  public Clob getClob(int columnIndex) {
    return getObject(columnIndex, Clob.class);
  }

  @Override
  public String getString(int columnIndex) {
    return getObject(columnIndex, String.class);
  }

  @Override
  public String toString() {
    String s = dataPath.toString();
    if (name != null) {
      s += "-" + name;
    }
    return s;
  }

  @Override
  public RelationDef getRuntimeRelationDef() {
    return this.getDataPath().getOrCreateRelationDef();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Object getObject(String columnName) {
    ColumnDef columnDef;
    try {
      columnDef = getRuntimeRelationDef().getColumnDef(columnName);
    } catch (NoColumnException e) {
      throw new RuntimeException("The column with the name (" + columnName + ") does not exist in the data resource (" + this.getDataPath() + ")");
    }
    return getObject(columnDef.getColumnPosition());
  }

  @Override
  public String getString(String columnName) {

    Object object = getObject(columnName);
    if (object == null) {
      return null;
    }
    return object.toString();

  }

}
