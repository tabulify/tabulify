package net.bytle.db.stream;

import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;

import java.sql.Clob;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface SelectStream extends AutoCloseable {

  boolean next();

  void close();

  String getString(int columnIndex);

  int getRow();


  Object getObject(int columnIndex);


  RelationDef getSelectDataDef();

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

}
