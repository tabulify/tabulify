package com.tabulify.stream;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.AttributeEnum;
import com.tabulify.conf.Origin;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.exception.CastException;
import com.tabulify.exception.NoColumnException;
import com.tabulify.exception.NoVariableException;
import com.tabulify.type.KeyNormalizer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class SelectStreamAbs implements Stream, SelectStream {


  private final DataPath dataPath;
  protected SelectStreamListener selectStreamListener = SelectStreamListener.of(this);
  private String name;


  public SelectStreamAbs(DataPath dataPath) {

    this.dataPath = dataPath;

  }

  @Override
  public Set<Attribute> getAttributes() {
    throw new UnsupportedOperationException("get attributes on select stream is not supported yet.");
  }

  @Override
  public Attribute getAttribute(AttributeEnum attribute) throws NoVariableException {
    return getAttribute(attribute.toKeyNormalizer());
  }

  @Override
  public Attribute getAttribute(KeyNormalizer name) throws NoVariableException {
    ColumnDef columnDef = this.getDataPath().getRelationDef().getColumnDef(name);
    return Attribute
      .createWithClassAndDefault(columnDef.getColumnNameNormalized(), Origin.DEFAULT, columnDef.getClazz(), null)
      .setPlainValue(this.getObject(columnDef));
  }

  @Override
  public DataPath toAttributesDataPath() {
    DataPath dataPath = this.getDataPath().getConnection().getTabular().getMemoryConnection()
      .getDataPath(this.getDataPath().getLogicalName() + "_stream_attributes");
    return toAttributesDataPath(dataPath);
  }

  /**
   * Retrieves and removes the head of this data path, or returns null if this queue is empty.
   *
   * @param timeout  - the numeric timeout to retrieve an object
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
    ColumnDef columnDef;
    try {
      columnDef = getRuntimeRelationDef().getColumnDef(columnName);
    } catch (NoColumnException e) {
      throw new IllegalArgumentException("The column (" + columnName + ") is unknown for the resource (" + this.getDataPath() + ")", e);
    }
    return this.getObject(columnDef, clazz);

  }

  @Override
  public <T> T getObject(KeyNormalizer columnName, Class<T> clazz) {

    ColumnDef columnDef = getRuntimeRelationDef().getColumnDef(columnName);
    if (columnDef == null) {
      throw new IllegalArgumentException("The column (" + columnName + ") is unknown for the resource (" + this.getDataPath() + ")");
    }
    return this.getObject(columnDef, clazz);

  }

  @Override
  public <T> T getObject(int index, Class<T> clazz) {
    ColumnDef<?> columnDef = getRuntimeRelationDef().getColumnDef(index);
    if (columnDef == null) {
      throw new IllegalArgumentException("The column position (" + index + ") is unknown for the resource (" + this.getDataPath() + ")");
    }
    return this.getObject(columnDef, clazz);
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
    ColumnDef<?> columnDef;
    try {
      columnDef = getRuntimeRelationDef().getColumnDef(columnName);
    } catch (NoColumnException e) {
      // You may check if a column exists
      return null;
    }
    return getObject(columnDef);
  }

  @Override
  public Object getObject(int columnPosition) {
    ColumnDef<?> columnDef;
    try {
      columnDef = getRuntimeRelationDef().getColumnDef(columnPosition);
    } catch (IllegalArgumentException e) {
      // Bad position is much more an error
      throw new RuntimeException("The column with the position (" + columnPosition + ") does not exist in the data resource (" + this.getDataPath() + ")");
    }
    return getObject(columnDef);
  }

  @Override
  public Object getObject(KeyNormalizer keyNormalizer) {
    ColumnDef<?> columnDef;
    try {
      columnDef = getRuntimeRelationDef().getColumnDef(keyNormalizer);
    } catch (IllegalArgumentException e) {
      return null;
    }
    return getObject(columnDef);
  }

  @Override
  public String getString(String columnName) {

    Object object = getObject(columnName);
    if (object == null) {
      return null;
    }
    return object.toString();

  }

  @Override
  public Integer getInteger(String columnName) {
    return getObject(columnName, Integer.class);
  }

  @Override
  public <T> T getObject(ColumnDef<?> columnDef, Class<T> clazz) {
    try {
      return this.getDataPath().getConnection().getObject(getObject(columnDef), clazz);
    } catch (CastException e) {
      throw new ClassCastException(e.getMessage());
    }
  }


}
