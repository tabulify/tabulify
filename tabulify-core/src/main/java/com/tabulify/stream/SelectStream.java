package com.tabulify.stream;

import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Meta;
import net.bytle.type.KeyNormalizer;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLXML;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A select stream is a cursor on record
 */
public interface SelectStream extends Stream, AutoCloseable, Meta {

  /**
   * @return true if a new record was found
   */
  boolean next();

  void close();

  boolean isClosed();

  /**
   * @param columnIndex - the column position
   *                    This function get the column def, calls {@link #getObject(ColumnDef)}
   *                    and return a string
   */
  String getString(int columnIndex);


  /**
   * @return the record id (ie row id in relation database)
   */
  long getRecordId();


  /**
   * @param columnIndex -  an index starting at one as {@link ResultSet#getObject(int)}
   * @return the object
   * This function get the column def and calls {@link #getObject(ColumnDef)}
   */
  Object getObject(int columnIndex);

  /**
   * @param columnDef - a column def
   * @return the object
   * This is the only function that should be implemented
   */
  Object getObject(ColumnDef columnDef);

  /**
   * This is a function that returns the data def at selection/runtime.
   * <p>
   * Executable Note:
   * If the {@link DataPath#getOrCreateRelationDef()} is empty because this is
   * an {@link DataPath#isRuntime()} executable and the user
   * did not give a {@link DataPath#mergeDataDefinitionFromYamlMap(Map) data def},
   * it's the only way to get the {@link RelationDef data definition}
   * <p>
   * Example:
   * * a query is build from the result set
   * * a text file without any defined structure will add a column called `lines`
   */
  RelationDef getRuntimeRelationDef();

  Double getDouble(int columnIndex);

  Clob getClob(int columnIndex);

  /**
   * @param timeout  - timeout to retrieve an object from a queue implementation
   * @param timeUnit - timeUnit of the timeout
   */
  boolean next(Integer timeout, TimeUnit timeUnit);

  Integer getInteger(int columnIndex);

  Object getObject(String columnName);

  SelectStreamListener getSelectStreamListener();

  <T> T getObject(int index, Class<T> clazz);

  /**
   * Wildcard is the only way so that Java will not check the type of object
   */
  List<?> getObjects();

  SelectStream setName(String name);

  /**
   * This function is used in two pass functions.
   * <p>
   * Example:
   * {@link Printer} will first scan the rows to calculate the layout then print
   */
  void beforeFirst();


  java.sql.Date getDate(int columnIndex);

  /**
   * Retrieves the value of the designated column in the current row
   * and will to the requested Java data type, if the conversion is supported.
   *
   * @param columnName the column name
   * @param clazz      the class
   * @param <T>        the t
   * @return the object cast
   * @throws ClassCastException if a cast error occurs
   */
  <T> T getObject(String columnName, Class<T> clazz);


  Timestamp getTimestamp(int columnIndex);

  Boolean getBoolean(int columnIndex);

  SQLXML getSqlXml(int columnIndex);

  java.sql.Time getTime(int columnIndex);


  String getString(String columnName);

  /**
   * Integer and not int because the value can be null
   */
  Integer getInteger(String columnName);

  <T> T getObject(KeyNormalizer columnName, Class<T> clazz);

  Object getObject(KeyNormalizer keyNormalizer);

  <T> T getObject(ColumnDef<?> columnDef, Class<T> clazz);

}
