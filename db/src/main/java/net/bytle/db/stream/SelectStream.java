package net.bytle.db.stream;

import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;

import java.sql.Clob;
import java.sql.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface SelectStream extends AutoCloseable {

  boolean next();

  void close();

  String getString(int columnIndex);

  long getRow();


  /**
   *
   * @param columnIndex -  an index starting at zero
   * @return
   */
  Object getObject(int columnIndex);


  /**
   *
   * This is a hook function to build the data def at selection/runtime
   *
   * This function is used when:
   *   * building the data def
   *   * or before running a select stream
   *
   * @param relationDef - the relationDef that must be build
   *
   * Example:
   *   * a query is build from the result set
   *   * a text file without any defined structure will add a column called `lines`
   *
   */
  void runtimeDataDef(RelationDef relationDef);

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

  List<Object> getObjects();

  SelectStream setName(String name);

  /**
   * This function is used in two pass functions.
   * <p>
   * Example:
   * {@link Streams#print(SelectStream)} will first scan the rows to calculate the layout then print
   */
  void beforeFirst();


  /**
   *
   * If the select stream execute a request before serving the stream
   * you can execute it explicitly with this function
   *
   * This is the case for instance with a query
   *
   * Some implementation may have a lazy execution (which means that it's only executed if needed)
   * but in case of long query, you may want to execute it explicitly in thread for instance.
   * This function is a good candidate for.
   */
  void execute();


  DataPath getDataPath();

  Date getDate(int columnIndex);

  /**
   * Retrieves the value of the designated column in the current row
   * and will to the requested Java data type, if the conversion is supported.
   * @param columnName
   * @param clazz
   * @param <T>
   * @return
   */
  <T> T getObject(String columnName, Class<T> clazz);

}
