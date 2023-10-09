package net.bytle.db.stream;

import net.bytle.db.model.RelationDef;

import java.sql.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface SelectStream extends Stream, AutoCloseable {

  /**
   *
   * @return true if a new record was found
   */
  boolean next();

  void close();


  String getString(int columnIndex);


  long getRow();


  /**
   *
   * @param columnIndex -  an index starting at one as {@link ResultSet#getObject(int)}
   * @return the object
   */
  Object getObject(int columnIndex);


  /**
   *
   * This is a function that returns the data def at selection/runtime
   *
   * This function is used before running a select stream.
   *
   *
   * Example:
   *   * a query is build from the result set
   *   * a text file without any defined structure will add a column called `lines`
   *
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

  List<?> getObjects();

  SelectStream setName(String name);

  /**
   * This function is used in two pass functions.
   * <p>
   * Example:
   * {@link Streams#print(SelectStream)} will first scan the rows to calculate the layout then print
   */
  void beforeFirst();





  Date getDate(int columnIndex);

  /**
   * Retrieves the value of the designated column in the current row
   * and will to the requested Java data type, if the conversion is supported.
   * @param columnName the column name
   * @param clazz the class
   * @param <T> the t
   * @return the object cast
   * @throws ClassCastException if a cast error occurs
   */
  <T> T getObject(String columnName, Class<T> clazz) ;


  Timestamp getTimestamp(int columnIndex);

  Boolean getBoolean(int columnIndex);

  SQLXML getSqlXml(int columnIndex);

  Time getTime(int columnIndex);


  String getString(String columnName);

}
